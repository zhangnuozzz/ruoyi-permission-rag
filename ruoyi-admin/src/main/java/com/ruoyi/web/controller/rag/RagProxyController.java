package com.ruoyi.web.controller.rag;

import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * RAG独立HTTP服务代理Controller
 *
 * @author fufu
 * @date 2026-05-13 12:37:47 CST
 */
@RestController
@RequestMapping("/rag/proxy/file")
public class RagProxyController
{
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${rag.service.url:http://localhost:8081}")
    private String ragServiceUrl;

    /**
     * 代理上传文件到独立rag_server服务
     */
    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file,
            @RequestParam("securityLevel") String securityLevel,
            @RequestParam("scopeCode") String scopeCode) throws IOException
    {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new MultipartInputResource(file));
        body.add("securityLevel", securityLevel);
        body.add("scopeCode", scopeCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        fillUserHeaders(headers);

        return restTemplate.postForEntity(ragServiceUrl + "/rag/file/upload", new HttpEntity<>(body, headers),
            String.class);
    }

    /**
     * 代理查询Milvus中存储的内容
     */
    @GetMapping("/milvus/list")
    public ResponseEntity<String> milvusList(@RequestParam Map<String, String> query)
    {
        return proxyGet("/rag/file/milvus/list", query);
    }

    /**
     * 代理查询MariaDB中存储的内容
     */
    @GetMapping("/mariadb/list")
    public ResponseEntity<String> mariadbList(@RequestParam Map<String, String> query)
    {
        return proxyGet("/rag/file/mariadb/list", query);
    }

    /**
     * 代理查询MinIO中存储的内容
     */
    @GetMapping("/minio/list")
    public ResponseEntity<String> minioList(@RequestParam Map<String, String> query)
    {
        return proxyGet("/rag/file/minio/list", query);
    }

    private ResponseEntity<String> proxyGet(String path, Map<String, String> query)
    {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(ragServiceUrl + path);
        query.forEach(builder::queryParam);
        HttpHeaders headers = new HttpHeaders();
        fillUserHeaders(headers);
        return restTemplate.exchange(builder.toUriString(), HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private void fillUserHeaders(HttpHeaders headers)
    {
        try
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser.getUserId() != null)
            {
                headers.add("X-User-Id", String.valueOf(loginUser.getUserId()));
            }
            headers.add("X-Username", loginUser.getUsername());
            if (loginUser.getDeptId() != null)
            {
                headers.add("X-Group-Id", String.valueOf(loginUser.getDeptId()));
            }
            if (loginUser.getUser() != null && loginUser.getUser().getDept() != null)
            {
                headers.add("X-Group-Name", loginUser.getUser().getDept().getDeptName());
            }
        }
        catch (RuntimeException ignored)
        {
            // Keep the proxy usable for authenticated test clients without a populated LoginUser.
        }
    }

    private static class MultipartInputResource extends ByteArrayResource
    {
        private final String filename;

        MultipartInputResource(MultipartFile file) throws IOException
        {
            super(file.getBytes());
            this.filename = file.getOriginalFilename();
        }

        @Override
        public String getFilename()
        {
            return filename;
        }
    }
}
