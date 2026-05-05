package com.ruoyi.system.service;

import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.system.domain.RagFileProcessResult;

/**
 * RAG文件处理服务
 *
 * @author fufu
 * @date 2026-05-05
 */
public interface IRagFileProcessService
{
    /**
     * 处理上传文件并分别写入Milvus、MariaDB和MinIO
     *
     * @param file 上传文件
     * @param securityLevel 文件密级
     * @param loginUser 登录用户
     * @return 文件处理结果
     * @throws Exception 处理失败
     */
    public RagFileProcessResult process(MultipartFile file, String securityLevel, LoginUser loginUser) throws Exception;
}
