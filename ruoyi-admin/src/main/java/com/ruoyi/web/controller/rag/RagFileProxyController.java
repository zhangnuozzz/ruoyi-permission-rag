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
import org.springframework.transaction.annotation.Transactional;
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
 * 3. 文档密级与国家秘密唯一知悉用户组校验；
 * 4. PUBLIC 不受用户组限制，INTERNAL 只做密级控制；
 * 5. SECRET / CONFIDENTIAL 必须绑定唯一知悉用户组；
 * 6. 将安全元数据同步写入 sys_rag_file / sys_rag_doc；
 * 7. 不支持解析的格式不允许假装入库成功。
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
        return performUpload(file, securityLevel, scopeCode, groupCode, request, false);
    }

    /** 公开文档无需用户组，内部文档只校验人员密级，国家秘密需本人所属知悉用户组。 */
    @PostMapping("/mine/upload")
    public AjaxResult uploadMine(@RequestParam("file") MultipartFile file,
                                 @RequestParam("securityLevel") String securityLevel,
                                 @RequestParam(value = "groupCode", required = false) String groupCode,
                                 HttpServletRequest request) throws IOException
    {
        String level = normalizeCode(securityLevel);
        if (!isSupportedLevel(level))
        {
            return AjaxResult.error("请选择有效的文档密级");
        }

        Long userId = SecurityUtils.getLoginUser().getUser().getUserId();
        if (!"PUBLIC".equals(level))
        {
            List<String> userLevels = jdbcTemplate.queryForList(
                    "select secret_level from sys_user_security_attr " +
                    "where user_id = ? and access_status = 'ACTIVE' limit 1",
                    String.class, userId);
            if (userLevels.isEmpty() || secretRank(level) > secretRank(userLevels.get(0)))
            {
                return AjaxResult.error("人员密级不足或访问状态未激活");
            }
        }

        if ("PUBLIC".equals(level) || "INTERNAL".equals(level))
        {
            return performUpload(file, level, level, "", request, true);
        }

        String selectedGroup = normalizeCode(groupCode);
        if (isBlank(selectedGroup) || "GROUP_PUBLIC".equals(selectedGroup))
        {
            return AjaxResult.error("秘密级、机密级文档必须选择本人所属的唯一知悉用户组");
        }
        Long memberships = jdbcTemplate.queryForObject(
                "select count(*) from sys_user_group_rel r join sys_group g on g.id = r.group_id " +
                "where r.user_id = ? and g.group_code = ? and g.status = '0' and g.del_flag = '0'",
                Long.class, userId, selectedGroup);
        if (memberships == null || memberships == 0)
        {
            return AjaxResult.error("只能选择本人所属的有效知悉用户组");
        }
        return performUpload(file, level, selectedGroup, selectedGroup, request, true);
    }

    /** 返回当前人员密级与国家秘密可用的知悉用户组。 */
    @GetMapping("/mine/groups")
    public AjaxResult mineGroups()
    {
        Long userId = SecurityUtils.getLoginUser().getUser().getUserId();
        List<String> levels = jdbcTemplate.queryForList(
                "select secret_level from sys_user_security_attr " +
                "where user_id = ? and access_status = 'ACTIVE' limit 1",
                String.class, userId);
        List<Map<String, Object>> groups = jdbcTemplate.queryForList(
                "select g.group_code groupCode, g.group_name groupName " +
                "from sys_user_group_rel r join sys_group g on g.id = r.group_id " +
                "where r.user_id = ? and g.status = '0' and g.del_flag = '0' " +
                "and g.group_code <> 'GROUP_PUBLIC' order by g.id",
                userId);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("userSecretLevel", levels.isEmpty() ? "PUBLIC" : normalizeCode(levels.get(0)));
        result.put("groups", groups);
        return AjaxResult.success(result);
    }

    /** 只列出本人上传的文件，不泄露 MinIO 对象名和其他用户文件。 */
    @GetMapping("/mine/list")
    public AjaxResult mineList(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                               @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize)
    {
        Long userId = SecurityUtils.getLoginUser().getUser().getUserId();
        int page = Math.max(1, Math.min(100000, pageNum == null ? 1 : pageNum));
        int size = Math.max(1, Math.min(50, pageSize == null ? 10 : pageSize));
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from sys_rag_file where upload_user_id = ?", Long.class, userId);
        List<Map<String, Object>> files = jdbcTemplate.queryForList(
                "select f.file_id fileId, f.file_name fileName, f.file_type fileType, " +
                "f.security_level securityLevel, f.scope_code scopeCode, f.group_name groupName, " +
                "f.chunk_count chunkCount, f.embedding_status embeddingStatus, " +
                "case when d.doc_id is null then 'UNAVAILABLE' " +
                "else coalesce(d.metadata_status, f.doc_status, 'ACTIVE') end metadataStatus, " +
                "date_format(f.create_time, '%Y-%m-%d %H:%i') createTime " +
                "from sys_rag_file f left join sys_rag_doc d on d.doc_id = f.file_id and d.del_flag = '0' " +
                "where f.upload_user_id = ? order by f.create_time desc, f.file_id desc limit ? offset ?",
                userId, size, (page - 1) * size);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", total == null ? 0L : total);
        result.put("rows", files);
        return AjaxResult.success(result);
    }

    /** 停用后平台二次权限过滤立即拒绝该文档；仅所有者可操作。 */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/mine/status")
    public AjaxResult changeMineStatus(@RequestParam("fileId") String fileId,
                                       @RequestParam("enabled") Boolean enabled)
    {
        if (isBlank(fileId) || enabled == null)
        {
            return AjaxResult.error("文件或操作状态无效");
        }
        Long userId = SecurityUtils.getLoginUser().getUser().getUserId();
        String status = enabled ? "ACTIVE" : "DISABLED";
        int updated = jdbcTemplate.update(
                "update sys_rag_doc d join sys_rag_file f on f.file_id = d.doc_id " +
                "set d.metadata_status = ?, d.status = ? " +
                "where f.file_id = ? and f.upload_user_id = ? and d.del_flag = '0'",
                status, enabled ? "0" : "1", fileId, userId);
        if (updated == 0)
        {
            return AjaxResult.error("文档不存在、无操作权限或权限标签未同步");
        }
        jdbcTemplate.update("update sys_rag_file set doc_status = ? where file_id = ? and upload_user_id = ?",
                status, fileId, userId);
        return AjaxResult.success("文档状态已更新");
    }

    private AjaxResult performUpload(MultipartFile file, String securityLevel, String scopeCode,
                                     String groupCode, HttpServletRequest request, boolean personal) throws IOException
    {
        long startTime = System.currentTimeMillis();

        String fileName = file == null ? "" : file.getOriginalFilename();
        String normalizedSecurityLevel = normalizeLevel(securityLevel);
        String normalizedScopeCode = normalizeCode(scopeCode);
        FileTypeDecision fileDecision = decideFileType(fileName);

        if (!isSupportedLevel(normalizeCode(securityLevel)))
        {
            return AjaxResult.error("请选择有效的文档密级");
        }

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

            if (personal)
            {
                Map<String, Object> uploaded = extractData(responseBody);
                String uploadedId = uploaded == null ? null : getString(uploaded, "fileId");
                Long synced = jdbcTemplate.queryForObject(
                        "select count(*) from sys_rag_doc where doc_id = ? and del_flag = '0'", Long.class, uploadedId);
                if (synced == null || synced == 0)
                {
                    return AjaxResult.error("文件已上传，但权限标签同步失败，请联系管理员处理，避免重复上传");
                }
            }

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
        String sql =
                "select " +
                "id, " +
                "group_code groupCode, " +
                "group_name groupName, " +
                "status " +
                "from sys_group " +
                "where del_flag = '0' " +
                "and status = '0' " +
                "order by id asc";
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
            doc.setUploadUserId(SecurityUtils.getLoginUser().getUser().getUserId());
            doc.setUploadUserName(SecurityUtils.getUsername());
            doc.setMetadataStatus("ACTIVE");
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
            metadata.put("vector_index_type", "AUTOINDEX");
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
                    "AUTOINDEX",
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
     * 根据新权限模型处理文档用户组。
     *
     * PUBLIC：
     * 不需要知悉用户组；
     * GROUP_PUBLIC 仅作为历史兼容元数据。
     *
     * INTERNAL：
     * 工作秘密，不绑定知悉用户组。
     *
     * SECRET / CONFIDENTIAL：
     * 国家秘密必须绑定唯一有效用户组。
     *
     * 用户组密级不参与文档准入判断。
     */
    private GroupDecision resolveAndCheckGroup(
            String securityLevel,
            String scopeCode,
            String groupCode)
    {
        String finalSecurityLevel =
                normalizeLevel(securityLevel);

        String finalGroupCode =
                normalizeCode(groupCode);

        /*
         * PUBLIC
         */
        if ("PUBLIC".equals(finalSecurityLevel))
        {
            return GroupDecision.allow(
                    finalSecurityLevel,
                    "PUBLIC",
                    "GROUP_PUBLIC",
                    "公开文档组",
                    null
            );
        }

        /*
         * INTERNAL
         *
         * 工作秘密只依赖人员密级，
         * 不需要知悉用户组。
         */
        if ("INTERNAL".equals(finalSecurityLevel))
        {
            return GroupDecision.allow(
                    finalSecurityLevel,
                    "INTERNAL",
                    "",
                    "",
                    null
            );
        }

        /*
         * SECRET / CONFIDENTIAL
         */
        if (isBlank(finalGroupCode))
        {
            return GroupDecision.deny(
                    "秘密级、机密级国家秘密必须选择唯一知悉用户组"
            );
        }

        if ("GROUP_PUBLIC".equals(finalGroupCode))
        {
            return GroupDecision.deny("公开文档兼容组不能作为国家秘密知悉用户组");
        }

        List<Map<String, Object>> groups =
                jdbcTemplate.queryForList(
                        "select group_code, group_name " +
                        "from sys_group " +
                        "where group_code=? " +
                        "and status='0' " +
                        "and del_flag='0' " +
                        "limit 1",
                        finalGroupCode
                );

        if (groups == null || groups.isEmpty())
        {
            return GroupDecision.deny(
                    "用户组不存在或未启用："
                    + finalGroupCode
            );
        }

        Map<String, Object> group =
                groups.get(0);

        String groupName =
                getString(
                        group,
                        "group_name"
                );

        /*
         * 国家秘密的知悉范围直接就是唯一用户组。
         *
         * scope_code 只保留兼容值，
         * 直接等于 groupCode，
         * 不再存在另一套映射关系。
         */
        return GroupDecision.allow(
                finalSecurityLevel,
                finalGroupCode,
                finalGroupCode,
                groupName,
                null
        );
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

    private boolean isSupportedLevel(String value)
    {
        return "PUBLIC".equals(value) || "INTERNAL".equals(value)
                || "SECRET".equals(value) || "CONFIDENTIAL".equals(value);
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
