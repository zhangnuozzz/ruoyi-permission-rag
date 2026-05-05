package com.ruoyi.system.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.RagFileChunk;
import com.ruoyi.system.domain.RagFileProcessResult;
import com.ruoyi.system.domain.SysRagFile;
import com.ruoyi.system.mapper.SysRagFileMapper;
import com.ruoyi.system.service.IRagFileProcessService;
import com.ruoyi.system.service.ISysDeptService;

/**
 * RAG文件处理服务实现
 *
 * @author fufu
 * @date 2026-05-05
 */
@Service
public class RagFileProcessServiceImpl implements IRagFileProcessService
{
    private final SysRagFileMapper sysRagFileMapper;
    private final ISysDeptService sysDeptService;
    private final MinioFileBackupService minioFileBackupService;
    private final LangChain4jDocumentChunkService langChain4jDocumentChunkService;
    private final MilvusChunkStoreService milvusChunkStoreService;

    public RagFileProcessServiceImpl(SysRagFileMapper sysRagFileMapper, ISysDeptService sysDeptService,
            MinioFileBackupService minioFileBackupService,
            LangChain4jDocumentChunkService langChain4jDocumentChunkService,
            MilvusChunkStoreService milvusChunkStoreService)
    {
        this.sysRagFileMapper = sysRagFileMapper;
        this.sysDeptService = sysDeptService;
        this.minioFileBackupService = minioFileBackupService;
        this.langChain4jDocumentChunkService = langChain4jDocumentChunkService;
        this.milvusChunkStoreService = milvusChunkStoreService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RagFileProcessResult process(MultipartFile file, String securityLevel, LoginUser loginUser) throws Exception
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("上传文件不能为空");
        }
        if (StringUtils.isBlank(securityLevel))
        {
            throw new ServiceException("文件密级不能为空");
        }
        if (loginUser == null)
        {
            throw new ServiceException("无法获取上传用户信息");
        }

        String fileId = UUID.randomUUID().toString().replace("-", "");
        String fileName = file.getOriginalFilename();
        byte[] rawContent = file.getBytes();
        UserGroupInfo groupInfo = resolveUserGroup(loginUser);
        String objectName = "backup/" + fileId + "/" + StringUtils.defaultString(fileName, "unnamed");

        minioFileBackupService.backup(rawContent, objectName,
            StringUtils.defaultIfBlank(file.getContentType(), "application/octet-stream"));

        String textContent = new String(rawContent, StandardCharsets.UTF_8);
        List<RagFileChunk> chunks = langChain4jDocumentChunkService.split(fileId, textContent, securityLevel,
            groupInfo.getGroupId(), groupInfo.getGroupName());
        milvusChunkStoreService.saveChunks(chunks);

        SysRagFile sysRagFile = new SysRagFile();
        sysRagFile.setFileId(fileId);
        sysRagFile.setFileName(fileName);
        sysRagFile.setUploadUserId(loginUser.getUserId());
        sysRagFile.setUploadUserName(loginUser.getUsername());
        sysRagFile.setSecurityLevel(securityLevel);
        sysRagFile.setGroupId(groupInfo.getGroupId());
        sysRagFile.setGroupName(groupInfo.getGroupName());
        sysRagFile.setMinioObjectName(objectName);
        sysRagFile.setCreateBy(loginUser.getUsername());
        sysRagFile.setCreateTime(new Date());
        sysRagFileMapper.insertSysRagFile(sysRagFile);

        RagFileProcessResult result = new RagFileProcessResult();
        result.setFileId(fileId);
        result.setFileName(fileName);
        result.setSecurityLevel(securityLevel);
        result.setGroupId(groupInfo.getGroupId());
        result.setGroupName(groupInfo.getGroupName());
        result.setMinioObjectName(objectName);
        result.setChunkCount(chunks.size());
        return result;
    }

    private UserGroupInfo resolveUserGroup(LoginUser loginUser)
    {
        Long deptId = loginUser.getDeptId();
        if (deptId != null)
        {
            SysDept dept = sysDeptService.selectDeptById(deptId);
            if (dept != null)
            {
                return new UserGroupInfo(String.valueOf(dept.getDeptId()), dept.getDeptName());
            }
            return new UserGroupInfo(String.valueOf(deptId), String.valueOf(deptId));
        }
        return new UserGroupInfo("default", "default");
    }

    private static class UserGroupInfo
    {
        private final String groupId;
        private final String groupName;

        UserGroupInfo(String groupId, String groupName)
        {
            this.groupId = groupId;
            this.groupName = groupName;
        }

        String getGroupId()
        {
            return groupId;
        }

        String getGroupName()
        {
            return groupName;
        }
    }
}
