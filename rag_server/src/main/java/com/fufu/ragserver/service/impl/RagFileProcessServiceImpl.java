package com.fufu.ragserver.service.impl;

import com.fufu.ragserver.domain.RagFileChunk;
import com.fufu.ragserver.domain.RagFileProcessResult;
import com.fufu.ragserver.domain.SysRagFile;
import com.fufu.ragserver.domain.UploadUserContext;
import com.fufu.ragserver.exception.ServiceException;
import com.fufu.ragserver.mapper.SysRagFileMapper;
import com.fufu.ragserver.service.IRagFileProcessService;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * RAG文件处理服务实现
 *
 * @author fufu
 * @date 2026-05-12
 */
@Service
public class RagFileProcessServiceImpl implements IRagFileProcessService
{
    private final SysRagFileMapper sysRagFileMapper;
    private final MinioFileBackupService minioFileBackupService;
    private final LangChain4jDocumentChunkService langChain4jDocumentChunkService;
    private final MilvusChunkStoreService milvusChunkStoreService;

    public RagFileProcessServiceImpl(SysRagFileMapper sysRagFileMapper,
            MinioFileBackupService minioFileBackupService,
            LangChain4jDocumentChunkService langChain4jDocumentChunkService,
            MilvusChunkStoreService milvusChunkStoreService)
    {
        this.sysRagFileMapper = sysRagFileMapper;
        this.minioFileBackupService = minioFileBackupService;
        this.langChain4jDocumentChunkService = langChain4jDocumentChunkService;
        this.milvusChunkStoreService = milvusChunkStoreService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RagFileProcessResult process(MultipartFile file, String securityLevel, String scopeCode,
            UploadUserContext userContext) throws Exception
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("上传文件不能为空");
        }
        if (StringUtils.isBlank(securityLevel))
        {
            throw new ServiceException("文件密级不能为空");
        }
        if (StringUtils.isBlank(scopeCode))
        {
            throw new ServiceException("权限标签不能为空");
        }

        UploadUserContext resolvedUser = resolveUserContext(userContext);
        String normalizedScopeCode = scopeCode.trim().toUpperCase(Locale.ROOT);
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String fileName = StringUtils.defaultIfBlank(file.getOriginalFilename(), "unnamed");
        byte[] rawContent = file.getBytes();
        String objectName = "backup/" + fileId + "/" + fileName;

        minioFileBackupService.backup(rawContent, objectName,
            StringUtils.defaultIfBlank(file.getContentType(), "application/octet-stream"));

        String textContent = new String(rawContent, StandardCharsets.UTF_8);
        List<RagFileChunk> chunks = langChain4jDocumentChunkService.split(fileId, textContent, securityLevel,
            normalizedScopeCode, resolvedUser.getGroupId(), resolvedUser.getGroupName());
        milvusChunkStoreService.saveChunks(chunks);

        SysRagFile sysRagFile = new SysRagFile();
        sysRagFile.setFileId(fileId);
        sysRagFile.setFileName(fileName);
        sysRagFile.setUploadUserId(resolvedUser.getUserId());
        sysRagFile.setUploadUserName(resolvedUser.getUsername());
        sysRagFile.setSecurityLevel(securityLevel);
        sysRagFile.setScopeCode(normalizedScopeCode);
        sysRagFile.setGroupId(resolvedUser.getGroupId());
        sysRagFile.setGroupName(resolvedUser.getGroupName());
        sysRagFile.setMinioObjectName(objectName);
        sysRagFile.setCreateBy(resolvedUser.getUsername());
        sysRagFile.setCreateTime(new Date());
        sysRagFileMapper.insertSysRagFile(sysRagFile);

        RagFileProcessResult result = new RagFileProcessResult();
        result.setFileId(fileId);
        result.setFileName(fileName);
        result.setSecurityLevel(securityLevel);
        result.setScopeCode(normalizedScopeCode);
        result.setGroupId(resolvedUser.getGroupId());
        result.setGroupName(resolvedUser.getGroupName());
        result.setMinioObjectName(objectName);
        result.setChunkCount(chunks.size());
        return result;
    }

    private UploadUserContext resolveUserContext(UploadUserContext userContext)
    {
        UploadUserContext resolved = userContext == null ? new UploadUserContext() : userContext;
        if (resolved.getUserId() == null)
        {
            resolved.setUserId(0L);
        }
        if (StringUtils.isBlank(resolved.getUsername()))
        {
            resolved.setUsername("anonymous");
        }
        if (StringUtils.isBlank(resolved.getGroupId()))
        {
            resolved.setGroupId("default");
        }
        if (StringUtils.isBlank(resolved.getGroupName()))
        {
            resolved.setGroupName(resolved.getGroupId());
        }
        return resolved;
    }
}
