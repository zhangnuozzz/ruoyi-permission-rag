package com.ruoyi.web.controller.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.service.IRagFileProcessService;

/**
 * RAG文件处理Controller
 *
 * @author fufu
 * @date 2026-05-05
 */
@RestController
@RequestMapping("/rag/file")
public class RagFileController extends BaseController
{
    @Autowired
    private IRagFileProcessService ragFileProcessService;

    /**
     * 上传文件并写入Milvus、MariaDB和MinIO
     */
    @PreAuthorize("@ss.hasPermi('rag:file:upload')")
    @Log(title = "RAG文件处理", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
            @RequestParam("securityLevel") String securityLevel) throws Exception
    {
        return success(ragFileProcessService.process(file, securityLevel, getLoginUser()));
    }
}
