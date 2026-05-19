package com.ruoyi.web.controller.rag;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * RAG文件服务代理控制器
 *
 * 若依平台侧负责权限、登录用户上下文和前端入口；
 * fufu RAG Server负责文件切分、MariaDB元数据、MinIO原文件、Milvus向量存储。
 */
@RestController
@RequestMapping("/rag/file")
public class RagFileProxyController
{
    @Value("${rag.server.url:http://localhost:8081}")
    private String ragServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 代理上传文件到 fufu RAG Server
     */
    @PreAuthorize("@ss.hasPermi('rag:file:upload')")
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam("securityLevel") String securityLevel,
                             @RequestParam("scopeCode") String scopeCode) throws IOException
    {
        String url = ragServerUrl + "/rag/file/upload";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        try
        {
            headers.add("X-User-Id", String.valueOf(SecurityUtils.getLoginUser().getUser().getUserId()));
            headers.add("X-Username", SecurityUtils.getUsername());
            headers.add("X-Group-Id", "default");
            headers.add("X-Group-Name", "default");
        }
        catch (Exception e)
        {
            headers.add("X-User-Id", "");
            headers.add("X-Username", "anonymous");
            headers.add("X-Group-Id", "default");
            headers.add("X-Group-Name", "default");
        }

        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes())
        {
            @Override
            public String getFilename()
            {
                return file.getOriginalFilename();
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("file", fileResource);
        body.add("securityLevel", securityLevel);
        body.add("scopeCode", scopeCode);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);

        try
        {
            ResponseEntity<Object> response = restTemplate.postForEntity(url, requestEntity, Object.class);
            return AjaxResult.success(response.getBody());
        }
        catch (Exception e)
        {
            return AjaxResult.error("调用RAG文件服务失败：" + e.getMessage());
        }
    }

    /**
     * 查询 fufu RAG Server 中 MariaDB 文件元数据
     */
    @PreAuthorize("@ss.hasPermi('rag:file:list')")
    @GetMapping("/mariadb/list")
    public AjaxResult mariadbList()
    {
        return proxyGet("/rag/file/mariadb/list");
    }

    /**
     * 查询 fufu RAG Server 中 Milvus 切块内容
     */
    @PreAuthorize("@ss.hasPermi('rag:file:list')")
    @GetMapping("/milvus/list")
    public AjaxResult milvusList(@RequestParam(value = "limit", defaultValue = "100") Integer limit)
    {
        return proxyGet("/rag/file/milvus/list?limit=" + limit);
    }

    /**
     * 查询 fufu RAG Server 中 MinIO 原始文件对象
     */
    @PreAuthorize("@ss.hasPermi('rag:file:list')")
    @GetMapping("/minio/list")
    public AjaxResult minioList(@RequestParam(value = "limit", defaultValue = "100") Integer limit)
    {
        return proxyGet("/rag/file/minio/list?limit=" + limit);
    }

    private AjaxResult proxyGet(String path)
    {
        try
        {
            Object result = restTemplate.getForObject(ragServerUrl + path, Object.class);
            return AjaxResult.success(result);
        }
        catch (Exception e)
        {
            return AjaxResult.error("调用RAG文件服务失败：" + e.getMessage());
        }
    }
}
