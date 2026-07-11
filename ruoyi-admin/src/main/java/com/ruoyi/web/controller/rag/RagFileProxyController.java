package com.ruoyi.web.controller.rag;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.system.domain.SysAccessLog;
import com.ruoyi.system.domain.SysRagDoc;
import com.ruoyi.system.service.ISysAccessLogService;
import com.ruoyi.system.service.ISysRagDocService;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * 平台侧职责：
 * 1. 上传前安全校验；
 * 2. 多格式识别；
 * 3. 文档密级、知悉范围、用户组绑定；
 * 4. 文件密级 <= 用户组密级；
 * 5. 将安全元数据同步写入 sys_rag_file / sys_rag_doc；
 * 6. 不支持解析的格式不允许假装入库成功。
 *
 * RAG Server职责：
 * 1. 原文件备份到 MinIO；
 * 2. 多格式文本抽取 / 图片 OCR / 文本切分；
 * 3. 向量写入 Milvus；
 * 4. 基础文件元数据写入 MariaDB。
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 代理上传文件到 RAG Server
     */
    @PreAuthorize("@ss.hasPermi('rag:file:upload')")
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam("securityLevel") String securityLevel,
                             @RequestParam("scopeCode") String scopeCode,
                             @RequestParam(value = "groupCode", required = false) String groupCode,
                             HttpServletRequest request) throws IOException
    {
        long startTime = System.currentTimeMillis();

        String fileName = file == null ? "" : file.getOriginalFilename();
        String normalizedSecurityLevel = normalizeLevel(securityLevel);
        String normalizedScopeCode = normalizeCode(scopeCode);
        FileTypeDecision fileDecision = decideFileType(fileName);

        if (file == null || file.isEmpty())
        {
            return AjaxResult.error("上传文件不能为空");
        }

        if (!fileDecision.allowUpload)
        {
            recordRagUploadAudit(request, fileName, normalizedSecurityLevel, normalizedScopeCode,
                    null, new RuntimeException(fileDecision.message), 0L);
            return AjaxResult.error(fileDecision.message);
        }

        GroupDecision groupDecision = resolveAndCheckGroup(normalizedSecurityLevel, normalizedScopeCode, groupCode);
        if (!groupDecision.allow)
        {
            recordRagUploadAudit(request, fileName, normalizedSecurityLevel, normalizedScopeCode,
                    null, new RuntimeException(groupDecision.message), 0L);
            return AjaxResult.error(groupDecision.message);
        }

        String url = ragServerUrl + "/rag/file/upload";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        try
        {
            headers.add("X-User-Id", String.valueOf(SecurityUtils.getLoginUser().getUser().getUserId()));
            headers.add("X-Username", SecurityUtils.getUsername());
            headers.add("X-Group-Id", groupDecision.groupCode);
            // HTTP Header 对中文不稳定，这里传 ASCII 编码的 groupCode，平台侧再回写真实中文组名。
            headers.add("X-Group-Name", groupDecision.groupCode);
        }
        catch (Exception e)
        {
            headers.add("X-User-Id", "");
            headers.add("X-Username", "anonymous");
            headers.add("X-Group-Id", groupDecision.groupCode);
            // HTTP Header 对中文不稳定，这里传 ASCII 编码的 groupCode，平台侧再回写真实中文组名。
            headers.add("X-Group-Name", groupDecision.groupCode);
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
        body.add("securityLevel", normalizedSecurityLevel);
        body.add("scopeCode", groupDecision.scopeCode);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);

        try
        {
            ResponseEntity<Object> response = restTemplate.postForEntity(url, requestEntity, Object.class);
            Object responseBody = response.getBody();

            String ragErrorMessage = extractRagErrorMessage(responseBody);
            if (!isBlank(ragErrorMessage))
            {
                RuntimeException exception = new RuntimeException(ragErrorMessage);
                recordRagUploadAudit(request, fileName, normalizedSecurityLevel, groupDecision.scopeCode,
                        responseBody, exception, System.currentTimeMillis() - startTime);
                return AjaxResult.error("RAG文件服务处理失败：" + ragErrorMessage);
            }

            syncRagDoc(responseBody, groupDecision);
            syncRagFileVectorMetadata(responseBody, fileDecision, groupDecision);

            recordRagUploadAudit(request, fileName, normalizedSecurityLevel, groupDecision.scopeCode,
                    responseBody, null, System.currentTimeMillis() - startTime);
            return AjaxResult.success(responseBody);
        }
        catch (Exception e)
        {
            recordRagUploadAudit(request, fileName, normalizedSecurityLevel, groupDecision.scopeCode,
                    null, e, System.currentTimeMillis() - startTime);
            return AjaxResult.error("调用RAG文件服务失败：" + e.getMessage());
        }
    }

    /**
     * 查询可绑定的有效用户组。
     */
    @PreAuthorize("@ss.hasPermi('rag:file:list')")
    @GetMapping("/groups")
    public AjaxResult groups()
    {
        String sql = "select id, group_code groupCode, group_name groupName, scope_code scopeCode, " +
                "group_secret_level groupSecretLevel, status " +
                "from sys_group where del_flag = '0' and status = '0' order by id asc";
        return AjaxResult.success(jdbcTemplate.queryForList(sql));
    }

    /**
     * 平台侧增强版 sys_rag_file 元数据。
     * RAG Server 原接口的 Domain 还没有新增字段，所以这里直接从平台数据库查完整字段。
     */
    @PreAuthorize("@ss.hasPermi('rag:file:list')")
    @GetMapping("/platform/mariadb/list")
    public AjaxResult platformMariadbList()
    {
        String sql = "select file_id fileId, file_name fileName, file_type fileType, parse_method parseMethod, " +
                "upload_user_id uploadUserId, upload_user_name uploadUserName, security_level securityLevel, " +
                "scope_code scopeCode, group_id groupId, group_name groupName, minio_object_name minioObjectName, " +
                "chunk_count chunkCount, embedding_status embeddingStatus, vector_index_type vectorIndexType, " +
                "metadata_index_status metadataIndexStatus, doc_level docLevel, doc_group docGroup, " +
                "doc_status docStatus, metadata_json metadataJson, create_by createBy, create_time createTime " +
                "from sys_rag_file order by create_time desc limit 100";
        return AjaxResult.success(jdbcTemplate.queryForList(sql));
    }

    /**
     * 查询 RAG Server 中 MariaDB 文件元数据
     */
    @PreAuthorize("@ss.hasPermi('rag:file:list')")
    @GetMapping("/mariadb/list")
    public AjaxResult mariadbList()
    {
        return proxyGet("/rag/file/mariadb/list");
    }

    /**
     * 查询 RAG Server 中 Milvus 切块内容
     */
    @PreAuthorize("@ss.hasPermi('rag:file:list')")
    @GetMapping("/milvus/list")
    public AjaxResult milvusList(@RequestParam(value = "limit", defaultValue = "100") Integer limit)
    {
        return proxyGet("/rag/file/milvus/list?limit=" + limit);
    }

    /**
     * 查询 RAG Server 中 MinIO 原始文件对象
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

    /**
     * 上传成功后，将 RAG Server 返回的文件元数据同步写入本平台 sys_rag_doc。
     */
    @SuppressWarnings("unchecked")
    private void syncRagDoc(Object responseBody, GroupDecision groupDecision)
    {
        try
        {
            Map<String, Object> data = extractData(responseBody);
            if (data == null)
            {
                return;
            }

            String fileId = getString(data, "fileId");
            String fileName = getString(data, "fileName");
            String securityLevel = getString(data, "securityLevel");
            String scopeCode = getString(data, "scopeCode");
            String minioObjectName = getString(data, "minioObjectName");
            String chunkCount = getString(data, "chunkCount");

            if (isBlank(fileId) || isBlank(fileName))
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
            doc.setScopeCode(isBlank(scopeCode) ? groupDecision.scopeCode : scopeCode);
            doc.setSecurityLevel(isBlank(securityLevel) ? groupDecision.securityLevel : securityLevel);
            doc.setOwnerGroupCode(groupDecision.groupCode);
            doc.setStatus("0");
            doc.setDelFlag("0");
            doc.setCreateBy(SecurityUtils.getUsername());
            doc.setRemark("RAG文件入库自动生成；MinIO对象：" + minioObjectName
                    + "；切块数：" + chunkCount
                    + "；doc_level=" + groupDecision.securityLevel
                    + "；doc_group=" + groupDecision.groupCode
                    + "；status=ACTIVE");

            sysRagDocService.insertSysRagDoc(doc);
        }
        catch (Exception e)
        {
            // 回写平台侧文档标签失败时，不影响 RAG Server 文件入库主链路
        }
    }

    /**
     * 回写 sys_rag_file 增强向量化元数据。
     */
    private void syncRagFileVectorMetadata(Object responseBody, FileTypeDecision fileDecision, GroupDecision groupDecision)
    {
        try
        {
            Map<String, Object> data = extractData(responseBody);
            if (data == null)
            {
                return;
            }

            String fileId = getString(data, "fileId");
            String chunkCount = getString(data, "chunkCount");
            if (isBlank(fileId))
            {
                return;
            }

            Integer chunks = parseInt(chunkCount);

            Map<String, Object> metadata = new LinkedHashMap<String, Object>();
            metadata.put("doc_level", groupDecision.securityLevel);
            metadata.put("doc_group", groupDecision.groupCode);
            metadata.put("status", "ACTIVE");
            metadata.put("scope_code", groupDecision.scopeCode);
            metadata.put("file_type", fileDecision.fileType);
            metadata.put("parse_method", fileDecision.parseMethod);
            metadata.put("embedding_status", "SUCCESS");
            metadata.put("vector_index_type", "HNSW");
            metadata.put("metadata_index_status", "READY");

            jdbcTemplate.update(
                    "update sys_rag_file set group_id=?, group_name=?, scope_code=?, security_level=?, " +
                            "file_type=?, parse_method=?, chunk_count=?, embedding_status=?, " +
                            "vector_index_type=?, metadata_index_status=?, doc_level=?, doc_group=?, doc_status=?, metadata_json=? " +
                            "where file_id=?",
                    groupDecision.groupCode,
                    groupDecision.groupName,
                    groupDecision.scopeCode,
                    groupDecision.securityLevel,
                    fileDecision.fileType,
                    fileDecision.parseMethod,
                    chunks,
                    "SUCCESS",
                    "HNSW",
                    "READY",
                    groupDecision.securityLevel,
                    groupDecision.groupCode,
                    "ACTIVE",
                    JSON.toJSONString(metadata),
                    fileId
            );
        }
        catch (Exception e)
        {
            // 增强元数据回写失败不影响主流程，但审计日志仍可定位问题
        }
    }

    /**
     * 格式识别与处理能力判断。
     */
    private FileTypeDecision decideFileType(String fileName)
    {
        String ext = extension(fileName);

        if ("TXT".equals(ext) || "MD".equals(ext) || "CSV".equals(ext) || "JSON".equals(ext) || "LOG".equals(ext))
        {
            return FileTypeDecision.allow(ext, "TIKA_TEXT", "文件格式支持，允许进入 RAG Server 文本抽取与向量化流程");
        }

        if ("PDF".equals(ext))
        {
            return FileTypeDecision.allow("PDF", "TIKA_TEXT_EXTRACTION",
                    "PDF格式支持，允许进入 RAG Server 文本抽取与向量化流程");
        }

        if ("DOC".equals(ext) || "DOCX".equals(ext))
        {
            return FileTypeDecision.allow(ext, "TIKA_TEXT_EXTRACTION",
                    "Word格式支持，允许进入 RAG Server 文本抽取与向量化流程");
        }

        if ("OFD".equals(ext))
        {
            return FileTypeDecision.allow("OFD", "OFD_TEXT_EXTRACTION",
                    "OFD格式支持，允许进入 RAG Server 文本抽取与向量化流程");
        }

        if ("PNG".equals(ext) || "JPG".equals(ext) || "JPEG".equals(ext) || "BMP".equals(ext)
                || "GIF".equals(ext) || "TIF".equals(ext) || "TIFF".equals(ext) || "WEBP".equals(ext))
        {
            return FileTypeDecision.allow(ext, "PORTABLE_TESSERACT_OCR",
                    "图片格式支持，允许进入 RAG Server OCR 与向量化流程");
        }

        return FileTypeDecision.deny(ext, "UNSUPPORTED",
                "暂不支持该文件格式：" + ext + "，已拒绝入库");
    }

    /**
     * 解析并校验文档绑定用户组。
     */
    private GroupDecision resolveAndCheckGroup(String securityLevel, String scopeCode, String groupCode)
    {
        String finalSecurityLevel = normalizeLevel(securityLevel);
        String finalScopeCode = normalizeCode(scopeCode);
        String finalGroupCode = normalizeCode(groupCode);

        if ("PUBLIC".equals(finalSecurityLevel))
        {
            finalScopeCode = "PUBLIC";
            finalGroupCode = "GROUP_PUBLIC";
        }

        if (isBlank(finalGroupCode))
        {
            return GroupDecision.deny("非公开文件必须选择唯一绑定用户组");
        }

        List<Map<String, Object>> groups = jdbcTemplate.queryForList(
                "select group_code, group_name, scope_code, group_secret_level " +
                        "from sys_group where group_code=? and status='0' and del_flag='0' limit 1",
                finalGroupCode
        );

        if (groups == null || groups.isEmpty())
        {
            return GroupDecision.deny("用户组不存在或未启用：" + finalGroupCode);
        }

        Map<String, Object> group = groups.get(0);
        String groupName = getString(group, "group_name");
        String groupScopeCode = getString(group, "scope_code");
        String groupSecretLevel = getString(group, "group_secret_level");

        if (!"PUBLIC".equals(finalSecurityLevel) && !isBlank(groupScopeCode))
        {
            finalScopeCode = groupScopeCode;
        }

        if (secretRank(groupSecretLevel) < secretRank(finalSecurityLevel))
        {
            return GroupDecision.deny("文件密级不能高于所绑定用户组密级：fileLevel="
                    + finalSecurityLevel + "，groupLevel=" + groupSecretLevel);
        }

        return GroupDecision.allow(finalSecurityLevel, finalScopeCode, finalGroupCode, groupName, groupSecretLevel);
    }

    /**
     * RAG 文件入库业务审计。
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(Object responseBody)
    {
        if (!(responseBody instanceof Map))
        {
            return null;
        }

        Map<String, Object> outer = (Map<String, Object>) responseBody;
        Object dataObj = outer.get("data");
        if (!(dataObj instanceof Map))
        {
            return null;
        }

        return (Map<String, Object>) dataObj;
    }

    @SuppressWarnings("unchecked")
    private String extractRagErrorMessage(Object responseBody)
    {
        if (!(responseBody instanceof Map))
        {
            return "RAG文件服务未返回有效响应";
        }

        Map<String, Object> outer = (Map<String, Object>) responseBody;
        Object code = outer.get("code");
        if (code != null && !"200".equals(String.valueOf(code)))
        {
            String msg = getString(outer, "msg");
            return isBlank(msg) ? "RAG文件服务返回失败，code=" + code : msg;
        }

        Object dataObj = outer.get("data");
        if (!(dataObj instanceof Map))
        {
            return "RAG文件服务未返回文件处理结果";
        }

        Map<String, Object> data = (Map<String, Object>) dataObj;
        if (isBlank(getString(data, "fileId")))
        {
            return "RAG文件服务返回结果缺少 fileId";
        }

        return null;
    }

    private String getString(Map<String, Object> map, String key)
    {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Integer parseInt(String value)
    {
        try
        {
            return Integer.parseInt(value);
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    private String normalizeLevel(String value)
    {
        String v = normalizeCode(value);
        if ("CONFIDENTIAL".equals(v) || "SECRET".equals(v) || "INTERNAL".equals(v) || "PUBLIC".equals(v))
        {
            return v;
        }
        return "INTERNAL";
    }

    private String normalizeCode(String value)
    {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String extension(String fileName)
    {
        if (isBlank(fileName) || !fileName.contains("."))
        {
            return "UNKNOWN";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).trim().toUpperCase(Locale.ROOT);
    }

    private int secretRank(String level)
    {
        if ("PUBLIC".equalsIgnoreCase(level)) return 1;
        if ("INTERNAL".equalsIgnoreCase(level)) return 2;
        if ("SECRET".equalsIgnoreCase(level)) return 3;
        if ("CONFIDENTIAL".equalsIgnoreCase(level)) return 4;
        return 0;
    }

    private boolean isBlank(String value)
    {
        return value == null || value.trim().length() == 0;
    }

    private static class FileTypeDecision
    {
        String fileType;
        String parseMethod;
        boolean allowUpload;
        String message;

        static FileTypeDecision allow(String fileType, String parseMethod, String message)
        {
            FileTypeDecision d = new FileTypeDecision();
            d.fileType = fileType;
            d.parseMethod = parseMethod;
            d.allowUpload = true;
            d.message = message;
            return d;
        }

        static FileTypeDecision deny(String fileType, String parseMethod, String message)
        {
            FileTypeDecision d = new FileTypeDecision();
            d.fileType = fileType;
            d.parseMethod = parseMethod;
            d.allowUpload = false;
            d.message = message;
            return d;
        }
    }

    private static class GroupDecision
    {
        boolean allow;
        String message;
        String securityLevel;
        String scopeCode;
        String groupCode;
        String groupName;
        String groupSecretLevel;

        static GroupDecision allow(String securityLevel, String scopeCode, String groupCode, String groupName, String groupSecretLevel)
        {
            GroupDecision d = new GroupDecision();
            d.allow = true;
            d.securityLevel = securityLevel;
            d.scopeCode = scopeCode;
            d.groupCode = groupCode;
            d.groupName = groupName;
            d.groupSecretLevel = groupSecretLevel;
            return d;
        }

        static GroupDecision deny(String message)
        {
            GroupDecision d = new GroupDecision();
            d.allow = false;
            d.message = message;
            return d;
        }
    }
}
