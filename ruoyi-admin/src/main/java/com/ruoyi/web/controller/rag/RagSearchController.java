package com.ruoyi.web.controller.rag;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.permission.PolicyDecisionResult;
import com.ruoyi.system.domain.rag.RagAuditLog;
import com.ruoyi.system.domain.rag.RagSearchRequest;
import com.ruoyi.system.domain.rag.RagSearchResult;
import com.ruoyi.system.service.IPermissionContextService;
import com.ruoyi.system.service.IPolicyDecisionService;
import com.ruoyi.system.service.IRagAuditLogService;
import com.ruoyi.system.service.IRagDocMockSearchService;
import com.ruoyi.system.service.IRagSecondFilterService;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RAG 安全检索入口控制器
 *
 * 当前版本完成：
 * 当前登录用户 -> 权限上下文 -> 策略决策 -> metadata filter 生成
 * -> 从 sys_rag_doc 读取模拟候选结果 -> 二次过滤 -> 审计日志记录
 */
@RestController
@RequestMapping("/rag")
public class RagSearchController
{
    @Autowired
    private IPermissionContextService permissionContextService;

    @Autowired
    private IPolicyDecisionService policyDecisionService;

    @Autowired
    private IRagAuditLogService ragAuditLogService;

    @Autowired
    private IRagSecondFilterService ragSecondFilterService;

    @Autowired
    private IRagDocMockSearchService ragDocMockSearchService;

    @Value("${rag.search.mode:remote}")
    private String ragSearchMode;

    @Value("${rag.search.remote-url:http://localhost:8081/rag/search}")
    private String ragSearchRemoteUrl;

    @Value("${rag.search.top-k:5}")
    private Integer defaultTopK;

    @Value("${rag.llm.enabled:true}")
    private Boolean ragLlmEnabled;

    @Value("${rag.llm.base-url:http://localhost:4000/v1}")
    private String ragLlmBaseUrl;

    @Value("${rag.llm.api-key:sk-local}")
    private String ragLlmApiKey;

    @Value("${rag.llm.model:local-model}")
    private String ragLlmModel;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * RAG 安全检索入口。
     *
     * 当前版本暂不真正调用向量数据库和大模型。
     * 先从 sys_rag_doc 表读取文档权限标签，模拟候选检索结果。
     *
     * @param request 检索请求
     * @return 预处理、策略决策与二次过滤结果
     */
    @PostMapping("/search")
    public AjaxResult search(@RequestBody RagSearchRequest request)
    {
        long startTime = System.currentTimeMillis();

        Long userId = SecurityUtils.getLoginUser().getUser().getUserId();
        String userName = SecurityUtils.getUsername();
        Boolean admin = SecurityUtils.isAdmin(userId);

        PermissionContext context = permissionContextService.buildContext(userId, userName, admin);
        PolicyDecisionResult decision = policyDecisionService.decide(context);

        if (Boolean.FALSE.equals(decision.getAllowAccess()))
        {
            long costTime = System.currentTimeMillis() - startTime;
            recordAuditLog(request, context, decision, costTime);

            Map<String, Object> result = buildBaseResult(request, context, decision, costTime);
            result.put("rawResultCount", 0);
            result.put("filteredResultCount", 0);
            result.put("rejectedResultCount", 0);
            result.put("rawResults", new ArrayList<RagSearchResult>());
            result.put("filteredResults", new ArrayList<RagSearchResult>());
            result.put("rejectedResults", new ArrayList<RagSearchResult>());

            AjaxResult ajax = AjaxResult.error("请求被权限策略拦截");
            ajax.put("data", result);
            return ajax;
        }

        List<RagSearchResult> rawResults = loadRawResults(request, decision, context);
        List<RagSearchResult> filteredResults = ragSecondFilterService.filter(context, rawResults);
        List<RagSearchResult> rejectedResults = buildRejectedResults(rawResults, filteredResults);
        String answer = buildAnswer(request, filteredResults);

        long costTime = System.currentTimeMillis() - startTime;

        recordAuditLog(request, context, decision, costTime);

        Map<String, Object> result = buildBaseResult(request, context, decision, costTime);
        result.put("searchMode", ragSearchMode);
        result.put("ragServerUrl", "remote".equalsIgnoreCase(ragSearchMode) ? ragSearchRemoteUrl : "");
        result.put("topK", resolveTopK(request));
        result.put("rawResultCount", rawResults == null ? 0 : rawResults.size());
        result.put("filteredResultCount", filteredResults == null ? 0 : filteredResults.size());
        result.put("rejectedResultCount", rejectedResults == null ? 0 : rejectedResults.size());
        result.put("rawResults", rawResults);
        result.put("filteredResults", filteredResults);
        result.put("rejectedResults", rejectedResults);
        result.put("answer", answer);
        result.put("llmEnabled", ragLlmEnabled);

        result.put("message", "请求已通过策略决策，完成RAG Server安全向量检索、权限二次过滤、审计留痕与模型回复");
        return (AjaxResult) AjaxResult.success(result);
    }

    private Map<String, Object> buildBaseResult(RagSearchRequest request, PermissionContext context,
                                                PolicyDecisionResult decision, long costTime)
    {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("query", request == null ? null : request.getQuery());
        result.put("userId", context.getUserId());
        result.put("userName", context.getUserName());
        result.put("admin", context.getAdmin());
        result.put("groupCodes", context.getGroupCodes());
        result.put("scopeCodes", context.getScopeCodes());
        result.put("policyCount", context.getPolicies() == null ? 0 : context.getPolicies().size());
        result.put("allowAccess", decision.getAllowAccess());
        result.put("denyReasons", decision.getDenyReasons());
        result.put("metadataFilter", decision.getMetadataFilter());
        result.put("decisionMessage", decision.getMessage());
        result.put("costTime", costTime);
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<RagSearchResult> loadRawResults(RagSearchRequest request, PolicyDecisionResult decision,
                                                 PermissionContext context)
    {
        if (!"remote".equalsIgnoreCase(ragSearchMode))
        {
            return ragDocMockSearchService.search(request.getQuery());
        }

        Map<String, Object> safeRequest = new LinkedHashMap<String, Object>();
        safeRequest.put("query", request.getQuery());
        safeRequest.put("topK", resolveTopK(request));
        safeRequest.put("metadataFilter", decision.getMetadataFilter());
        safeRequest.put("scopeCodes", decision.getScopeCodes());
        safeRequest.put("userId", context.getUserId());
        safeRequest.put("userName", context.getUserName());

        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> response = restTemplate.postForEntity(ragSearchRemoteUrl,
                    new HttpEntity<Map<String, Object>>(safeRequest, headers), Map.class);
            Object body = response.getBody();
            if (!(body instanceof Map))
            {
                return new ArrayList<RagSearchResult>();
            }
            Object data = ((Map<String, Object>) body).get("data");
            if (!(data instanceof List))
            {
                return new ArrayList<RagSearchResult>();
            }
            return convertResults((List<Object>) data);
        }
        catch (Exception e)
        {
            throw new RuntimeException("调用RAG Server检索失败：" + e.getMessage(), e);
        }
    }

    private Integer resolveTopK(RagSearchRequest request)
    {
        if (request != null && request.getTopK() != null && request.getTopK() > 0)
        {
            return request.getTopK();
        }
        return defaultTopK == null || defaultTopK <= 0 ? 5 : defaultTopK;
    }

    @SuppressWarnings("unchecked")
    private List<RagSearchResult> convertResults(List<Object> rows)
    {
        List<RagSearchResult> results = new ArrayList<RagSearchResult>();
        for (Object row : rows)
        {
            if (!(row instanceof Map))
            {
                continue;
            }
            Map<String, Object> map = (Map<String, Object>) row;
            RagSearchResult result = new RagSearchResult();
            result.setDocId(getString(map, "docId"));
            result.setChunkId(getString(map, "chunkId"));
            result.setTitle(getString(map, "title"));
            result.setContent(getString(map, "content"));
            result.setSummary(getString(map, "summary"));
            result.setScopeCode(getString(map, "scopeCode"));
            result.setLevel(getString(map, "level"));
            result.setPassed(false);
            result.setFilterReason("");
            Object score = map.get("score");
            if (score instanceof Number)
            {
                result.setScore(((Number) score).floatValue());
            }
            results.add(result);
        }
        return results;
    }

    private String getString(Map<String, Object> map, String key)
    {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String buildAnswer(RagSearchRequest request, List<RagSearchResult> filteredResults)
    {
        if (filteredResults == null || filteredResults.isEmpty())
        {
            return "未检索到当前权限范围内可用于回答的文档片段。";
        }
        if (!Boolean.TRUE.equals(ragLlmEnabled))
        {
            return "已完成权限过滤，当前未启用本地文本模型调用。";
        }
        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(ragLlmApiKey);

            List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
            messages.add(message("system", "你是一个基于企业知识库回答问题的助手。只能依据提供的已授权文档片段作答；如果材料不足，请明确说明。"));
            messages.add(message("user", buildPrompt(request == null ? "" : request.getQuery(), filteredResults)));

            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("model", ragLlmModel);
            body.put("messages", messages);
            body.put("temperature", 0.2);

            try
            {
                ResponseEntity<Map> response = restTemplate.postForEntity(resolveChatCompletionsUrl(),
                        new HttpEntity<Map<String, Object>>(body, headers), Map.class);
                return parseOpenAiAnswer(response.getBody());
            }
            catch (Exception e)
            {
                if (isGroqForbidden(e))
                {
                    return callOpenAiByCurl(body);
                }
                throw e;
            }
        }
        catch (Exception e)
        {
            return "已完成权限过滤，但调用本地文本模型失败：" + e.getMessage();
        }
    }

    private Map<String, String> message(String role, String content)
    {
        Map<String, String> message = new LinkedHashMap<String, String>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String buildPrompt(String query, List<RagSearchResult> filteredResults)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("用户问题：").append(query).append("\n\n已授权文档片段：\n");
        for (int i = 0; i < filteredResults.size(); i++)
        {
            RagSearchResult item = filteredResults.get(i);
            builder.append("[").append(i + 1).append("] 文档ID：").append(item.getDocId())
                    .append("，权限标签：").append(item.getScopeCode())
                    .append("\n").append(item.getContent()).append("\n\n");
        }
        return builder.toString();
    }

    private String resolveChatCompletionsUrl()
    {
        String baseUrl = ragLlmBaseUrl == null ? "" : ragLlmBaseUrl.trim();
        if (baseUrl.endsWith("/chat/completions"))
        {
            return baseUrl;
        }
        while (baseUrl.endsWith("/"))
        {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/chat/completions";
    }

    private boolean isGroqForbidden(Exception e)
    {
        String message = e == null ? "" : String.valueOf(e.getMessage());
        String baseUrl = ragLlmBaseUrl == null ? "" : ragLlmBaseUrl;
        return baseUrl.contains("api.groq.com") && (message.contains("403") || message.contains("Forbidden"));
    }

    @SuppressWarnings("unchecked")
    private String callOpenAiByCurl(Map<String, Object> body) throws Exception
    {
        File configFile = File.createTempFile("groq-curl-", ".conf");
        File bodyFile = File.createTempFile("groq-body-", ".json");
        try
        {
            Files.write(bodyFile.toPath(), JSON.toJSONString(body).getBytes(StandardCharsets.UTF_8));

            StringBuilder config = new StringBuilder();
            config.append("silent\n");
            config.append("show-error\n");
            config.append("request = \"POST\"\n");
            config.append("url = \"").append(escapeCurlConfig(resolveChatCompletionsUrl())).append("\"\n");
            config.append("header = \"Content-Type: application/json\"\n");
            config.append("header = \"Authorization: Bearer ").append(escapeCurlConfig(ragLlmApiKey)).append("\"\n");
            config.append("data-binary = \"@").append(escapeCurlConfig(bodyFile.getAbsolutePath())).append("\"\n");
            Files.write(configFile.toPath(), config.toString().getBytes(StandardCharsets.UTF_8));

            Process process = new ProcessBuilder("curl", "--config", configFile.getAbsolutePath()).start();
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished)
            {
                process.destroyForcibly();
                throw new RuntimeException("curl fallback 调用超时");
            }

            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());
            if (process.exitValue() != 0)
            {
                throw new RuntimeException("curl fallback 调用失败：" + stderr);
            }

            Map response = JSON.parseObject(stdout, Map.class);
            return parseOpenAiAnswer(response);
        }
        finally
        {
            configFile.delete();
            bodyFile.delete();
        }
    }

    private String escapeCurlConfig(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String readStream(InputStream inputStream) throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = inputStream.read(buffer)) != -1)
        {
            outputStream.write(buffer, 0, length);
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private String parseOpenAiAnswer(Map body)
    {
        if (body == null)
        {
            return "";
        }
        Object choicesObj = body.get("choices");
        if (!(choicesObj instanceof List) || ((List) choicesObj).isEmpty())
        {
            return String.valueOf(body);
        }
        Object first = ((List) choicesObj).get(0);
        if (!(first instanceof Map))
        {
            return String.valueOf(first);
        }
        Object messageObj = ((Map) first).get("message");
        if (messageObj instanceof Map)
        {
            Object content = ((Map) messageObj).get("content");
            return content == null ? "" : String.valueOf(content);
        }
        Object text = ((Map) first).get("text");
        return text == null ? "" : String.valueOf(text);
    }

    /**
     * 根据原始候选结果和过滤后结果，计算被二次过滤拦截的文档。
     *
     * 该结果主要用于前端演示和审计解释：
     * 1. rawResults 表示模拟向量检索命中的候选文档；
     * 2. filteredResults 表示权限过滤后允许返回给用户的文档；
     * 3. rejectedResults 表示命中但因权限不匹配被过滤的文档。
     */
    private List<RagSearchResult> buildRejectedResults(List<RagSearchResult> rawResults, List<RagSearchResult> filteredResults)
    {
        List<RagSearchResult> rejectedResults = new ArrayList<RagSearchResult>();

        if (rawResults == null || rawResults.isEmpty())
        {
            return rejectedResults;
        }

        for (RagSearchResult raw : rawResults)
        {
            boolean passed = false;

            if (filteredResults != null)
            {
                for (RagSearchResult filtered : filteredResults)
                {
                    if (raw.getDocId() != null && raw.getDocId().equals(filtered.getDocId()))
                    {
                        passed = true;
                        break;
                    }
                }
            }

            if (!passed)
            {
                raw.setPassed(false);
                if (raw.getFilterReason() == null || raw.getFilterReason().length() == 0)
                {
                    raw.setFilterReason("文档 scopeCode 不在当前用户可访问范围内，已被二次权限过滤拦截");
                }
                rejectedResults.add(raw);
            }
        }

        return rejectedResults;
    }

    /**
     * 记录 RAG 检索审计日志。
     */
    private void recordAuditLog(RagSearchRequest request, PermissionContext context,
                                PolicyDecisionResult decision, long costTime)
    {
        RagAuditLog auditLog = new RagAuditLog();

        auditLog.setUserId(context.getUserId());
        auditLog.setUserName(context.getUserName());
        auditLog.setQueryText(request == null ? null : request.getQuery());
        auditLog.setGroupCodes(joinList(context.getGroupCodes()));
        auditLog.setScopeCodes(joinList(context.getScopeCodes()));
        auditLog.setMetadataFilter(decision.getMetadataFilter());
        auditLog.setAllowAccess(Boolean.TRUE.equals(decision.getAllowAccess()) ? "0" : "1");
        auditLog.setDenyReasons(joinList(decision.getDenyReasons()));
        auditLog.setCostTime(costTime);

        ragAuditLogService.record(auditLog);
    }

    /**
     * 将字符串列表转换为逗号分隔字符串。
     */
    private String joinList(List<String> list)
    {
        if (list == null || list.isEmpty())
        {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++)
        {
            if (i > 0)
            {
                builder.append(",");
            }
            builder.append(list.get(i));
        }
        return builder.toString();
    }
}
