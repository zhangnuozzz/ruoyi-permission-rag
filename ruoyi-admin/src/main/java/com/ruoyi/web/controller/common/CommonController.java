package com.ruoyi.web.controller.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.util.StreamUtils;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.file.ExportFileStore;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.framework.config.ServerConfig;

/**
 * 通用请求处理
 * 
 * @author ruoyi
 */
@RestController
public class CommonController
{
    private static final Logger log = LoggerFactory.getLogger(CommonController.class);

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private ExportFileStore exportFileStore;

    /**
     * 通用下载请求
     * 
     * @param fileName 文件名称
     * @param delete 是否删除
     */
    @GetMapping("common/download")
    public void fileDownload(String fileName, Boolean delete, HttpServletResponse response, HttpServletRequest request)
            throws IOException
    {
        final Path file;
        try
        {
            file = exportFileStore.authorize(fileName);
        }
        catch (CustomException exception)
        {
            response.sendError(exception.getCode() == null ? 403 : exception.getCode(), exception.getMessage());
            return;
        }
        catch (Exception exception)
        {
            log.error("校验导出文件失败", exception);
            response.sendError(503, "下载校验暂时不可用，请稍后重试。");
            return;
        }
        try (InputStream input = Files.newInputStream(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))
        {
            String displayName = fileName.substring(fileName.indexOf('_') + 1);
            String encodedName = URLEncoder.encode(displayName, "UTF-8").replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setContentLengthLong(Files.size(file));
            StreamUtils.copy(input, response.getOutputStream());
            response.flushBuffer();
        }
        catch (IOException exception)
        {
            log.warn("传输导出文件失败", exception);
            if (!response.isCommitted())
            {
                response.reset();
                response.sendError(500, "文件下载失败，请重新导出。");
            }
            return;
        }
        if (Boolean.TRUE.equals(delete))
        {
            try { exportFileStore.remove(fileName); }
            catch (Exception exception) { log.warn("清理已下载导出文件失败，将由过期任务清理", exception); }
        }
    }

    /**
     * 通用上传请求
     */
    @PostMapping("/common/upload")
    public AjaxResult uploadFile(MultipartFile file) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            // 上传并返回新文件名称
            String fileName = FileUploadUtils.upload(filePath, file);
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("fileName", fileName);
            ajax.put("url", url);
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 本地资源通用下载
     */
    @GetMapping("/common/download/resource")
    public void resourceDownload(String name, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.sendError(403, "请通过对应业务接口访问文件。");
    }
}
