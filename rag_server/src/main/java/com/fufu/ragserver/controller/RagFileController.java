package com.fufu.ragserver.controller;

import com.fufu.ragserver.domain.AjaxResult;
import com.fufu.ragserver.domain.SysRagFile;
import com.fufu.ragserver.domain.UploadUserContext;
import com.fufu.ragserver.mapper.SysRagFileMapper;
import com.fufu.ragserver.service.IRagFileProcessService;
import com.fufu.ragserver.service.impl.MilvusChunkStoreService;
import com.fufu.ragserver.service.impl.MinioFileBackupService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * RAG文件处理Controller
 *
 * @author fufu
 * @date 2026-05-13 12:37:47 CST
 */
@RestController
@RequestMapping("/rag/file")
public class RagFileController
{
    private final IRagFileProcessService ragFileProcessService;
    private final SysRagFileMapper sysRagFileMapper;
    private final MilvusChunkStoreService milvusChunkStoreService;
    private final MinioFileBackupService minioFileBackupService;

    public RagFileController(IRagFileProcessService ragFileProcessService, SysRagFileMapper sysRagFileMapper,
            MilvusChunkStoreService milvusChunkStoreService, MinioFileBackupService minioFileBackupService)
    {
        this.ragFileProcessService = ragFileProcessService;
        this.sysRagFileMapper = sysRagFileMapper;
        this.milvusChunkStoreService = milvusChunkStoreService;
        this.minioFileBackupService = minioFileBackupService;
    }

    /**
     * 上传文件并写入Milvus、MariaDB和MinIO
     */
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
            @RequestParam("securityLevel") String securityLevel,
            @RequestParam("scopeCode") String scopeCode,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Group-Id", required = false) String groupId,
            @RequestHeader(value = "X-Group-Name", required = false) String groupName) throws Exception
    {
        UploadUserContext userContext = new UploadUserContext();
        userContext.setUserId(parseUserId(userId));
        userContext.setUsername(username);
        userContext.setGroupId(groupId);
        userContext.setGroupName(groupName);
        return AjaxResult.success(ragFileProcessService.process(file, securityLevel, scopeCode, userContext));
    }

    /**
     * 查询MariaDB中存储的RAG文件元数据
     */
    @GetMapping("/mariadb/list")
    public AjaxResult mariadbList(SysRagFile query)
    {
        return AjaxResult.success(sysRagFileMapper.selectSysRagFileList(query));
    }

    /**
     * 查询Milvus中存储的切块内容
     */
    @GetMapping("/milvus/list")
    public AjaxResult milvusList(@RequestParam(value = "limit", defaultValue = "100") Integer limit)
    {
        return AjaxResult.success(milvusChunkStoreService.listChunks(limit));
    }

    /**
     * 查询MinIO中存储的原始文件对象
     */
    @GetMapping("/minio/list")
    public AjaxResult minioList(@RequestParam(value = "limit", defaultValue = "100") Integer limit) throws Exception
    {
        return AjaxResult.success(minioFileBackupService.listObjects(limit));
    }

    private Long parseUserId(String userId)
    {
        if (StringUtils.isBlank(userId))
        {
            return null;
        }
        return Long.valueOf(userId);
    }
}
