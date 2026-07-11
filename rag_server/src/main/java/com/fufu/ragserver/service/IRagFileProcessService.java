package com.fufu.ragserver.service;

import com.fufu.ragserver.domain.RagFileProcessResult;
import com.fufu.ragserver.domain.UploadUserContext;
import org.springframework.web.multipart.MultipartFile;

/**
 * RAG文件处理服务
 *
 * @author fufu
 * @date 2026-05-12
 */
public interface IRagFileProcessService
{
    RagFileProcessResult process(MultipartFile file, String securityLevel, String scopeCode,
            UploadUserContext userContext) throws Exception;
}
