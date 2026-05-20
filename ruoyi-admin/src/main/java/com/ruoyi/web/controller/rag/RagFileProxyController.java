package com.ruoyi.web.controller.rag;

import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.ruoyi.system.domain.SysRagDoc;
import com.ruoyi.system.service.ISysRagDocService;
import com.ruoyi.system.service.ISysAccessLogService;
import com.ruoyi.system.domain.SysAccessLog;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.DateUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private ISysRagDocService sysRagDocService;

    @Autowired
    private ISysAccessLogService sysAccessLogService;

    /**
     * 代理上传文件到 fufu RAG Server
     */
    @PreAuthorize("@ss.hasPermi('rag:file:upload')")
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam("securityLevel") String securityLevel,
                             @RequestParam("scopeCode") String scopeCode,
                             HttpServletRequest request) throws IOException
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
            long startTime = System.currentTimeMillis();
            ResponseEntity<Object> response = restTemplate.postForEntity(url, requestEntity, Object.class);
            Object responseBody = response.getBody();
            syncRagDoc(responseBody);
            recordRagUploadAudit(request, file.getOriginalFilename(), securityLevel, scopeCode, responseBody, null, System.currentTimeMillis() - startTime);
            return AjaxResult.success(responseBody);
        }
        catch (Exception e)
        {
            recordRagUploadAudit(request, file.getOriginalFilename(), securityLevel, scopeCode, null, e, 0L);
            return AjaxResult.error("调用RAG文件服务失败：" + e.getMessage());
        }
    }

    /**
     * RAG 文件入库业务审计。
     *
     * 普通访问日志会记录一次 /rag/file/upload；
     * 这里额外记录一条 /rag/file/upload#audit，用于突出 RAG 入库业务行为。
     */
    @SuppressWarnings("unchecked")
    private void recordRagUploadAudit(HttpServletRequest request, String fileName, String securityLevel, String scopeCode,
                                      Object responseBody, Exception exception, Long costTime)
    {
        try
        {
            SysAccessLog log = new SysAccessLog();

            try
            {
                log.setUserId(SecurityUtils.getLoginUser().getUser().getUserId());
                log.setUserName(SecurityUtils.getUsername());
            }
            catch (Exception e)
            {
                log.setUserId(0L);
                log.setUserName("anonymous");
            }

            log.setIpaddr(IpUtils.getIpAddr(request));
            log.setRequestUri("/rag/file/upload#audit");
            log.setRequestMethod("POST");
            log.setCostTime(costTime == null ? 0L : costTime);
            log.setCreateTime(DateUtils.getNowDate());

            if (exception == null)
            {
                log.setStatus("0");

                String fileId = "";
                String minioObjectName = "";
                String chunkCount = "";

                if (responseBody instanceof Map)
                {
                    Map<String, Object> outer = (Map<String, Object>) responseBody;
                    Object dataObj = outer.get("data");
                    if (dataObj instanceof Map)
                    {
                        Map<String, Object> data = (Map<String, Object>) dataObj;
                        fileId = getString(data, "fileId");
                        minioObjectName = getString(data, "minioObjectName");
                        chunkCount = getString(data, "chunkCount");
                    }
                }

                log.setErrorMsg("RAG_FILE_UPLOAD_SUCCESS"
                        + "；fileName=" + fileName
                        + "；fileId=" + fileId
                        + "；securityLevel=" + securityLevel
                        + "；scopeCode=" + scopeCode
                        + "；minioObjectName=" + minioObjectName
                        + "；chunkCount=" + chunkCount);
            }
            else
            {
                log.setStatus("1");
                log.setErrorMsg("RAG_FILE_UPLOAD_FAIL"
                        + "；fileName=" + fileName
                        + "；securityLevel=" + securityLevel
                        + "；scopeCode=" + scopeCode
                        + "；error=" + exception.getMessage());
            }

            sysAccessLogService.insertSysAccessLog(log);
        }
        catch (Exception e)
        {
            // 审计失败不影响上传主流程
        }
    }

    /**
     * 上传成功后，将 fufu RAG Server 返回的文件元数据同步写入本平台 sys_rag_doc。
     *
     * 这样文档入库、权限标签、后续检索过滤可以形成平台侧闭环。
     */
    @SuppressWarnings("unchecked")
    private void syncRagDoc(Object responseBody)
    {
        try
        {
            if (!(responseBody instanceof Map))
            {
                return;
            }

            Map<String, Object> outer = (Map<String, Object>) responseBody;
            Object dataObj = outer.get("data");
            if (!(dataObj instanceof Map))
            {
                return;
            }

            Map<String, Object> data = (Map<String, Object>) dataObj;

            String fileId = getString(data, "fileId");
            String fileName = getString(data, "fileName");
            String securityLevel = getString(data, "securityLevel");
            String scopeCode = getString(data, "scopeCode");
            String groupId = getString(data, "groupId");
            String minioObjectName = getString(data, "minioObjectName");
            String chunkCount = getString(data, "chunkCount");

            if (fileId == null || fileId.length() == 0 || fileName == null || fileName.length() == 0)
            {
                return;
            }

            SysRagDoc query = new SysRagDoc();
            query.setDocId(fileId);
            List<SysRagDoc> exists = sysRagDocService.selectSysRagDocList(query);
            if (exists != null && !exists.isEmpty())
            {
                return;
            }

            SysRagDoc doc = new SysRagDoc();
            doc.setDocId(fileId);
            doc.setDocName(fileName);
            doc.setScopeCode(scopeCode == null || scopeCode.length() == 0 ? "INTERNAL" : scopeCode);
            doc.setSecurityLevel(securityLevel == null || securityLevel.length() == 0 ? "INTERNAL" : securityLevel);
            doc.setOwnerGroupCode(groupId == null || groupId.length() == 0 ? "default" : groupId);
            doc.setStatus("0");
            doc.setDelFlag("0");
            doc.setCreateBy(SecurityUtils.getUsername());
            doc.setRemark("RAG文件入库自动生成；MinIO对象：" + minioObjectName + "；切块数：" + chunkCount);

            sysRagDocService.insertSysRagDoc(doc);
        }
        catch (Exception e)
        {
            // 回写平台侧文档标签失败时，不影响 fufu RAG Server 文件入库主链路
        }
    }

    private String getString(Map<String, Object> map, String key)
    {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
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
