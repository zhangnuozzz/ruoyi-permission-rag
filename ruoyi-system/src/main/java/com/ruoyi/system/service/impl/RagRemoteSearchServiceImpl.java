package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.permission.PolicyDecisionResult;
import com.ruoyi.system.domain.rag.RagSearchRequest;
import com.ruoyi.system.domain.rag.RagSearchResult;
import com.ruoyi.system.service.IRagRemoteSearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG Server 远程真实检索 Service 实现
 *
 * 说明：
 * 1. 平台侧会生成完整 VACP metadataFilter，用于审计与二次过滤；
 * 2. fufu RAG Server 当前 Milvus collection 暂时不包含 metadata_status、security_level 等字段；
 * 3. 因此发给 8081 的 metadataFilter 使用 Milvus 已存在字段 scope_code 做粗过滤；
 * 4. 平台 8080 再继续执行精细二次过滤，保证安全链路完整。
 */
@Service
public class RagRemoteSearchServiceImpl implements IRagRemoteSearchService
{
    @Value("${rag.server.url:http://localhost:8081}")
    private String ragServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @SuppressWarnings("unchecked")
    public List<RagSearchResult> search(RagSearchRequest request, PermissionContext context, PolicyDecisionResult decision)
    {
        List<RagSearchResult> results = new ArrayList<RagSearchResult>();

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("query", request == null ? null : request.getQuery());
        body.put("topK", request == null || request.getTopK() == null ? 5 : request.getTopK());

        if (context != null)
        {
            Map<String, Object> userContext = new LinkedHashMap<String, Object>();
            userContext.put("userId", context.getUserId());
            userContext.put("userName", context.getUserName());
            userContext.put("admin", context.getAdmin());
            userContext.put("groupCodes", context.getGroupCodes());
            userContext.put("scopeCodes", context.getScopeCodes());
            body.put("userContext", userContext);

            body.put("scopeCodes", context.getScopeCodes());
        }

        // 发给 8081 的是 Milvus 当前 collection 能识别的粗过滤表达式。
        body.put("metadataFilter", buildMilvusCompatibleFilter(context));

        // 平台完整过滤表达式仍保留，方便 RAG Server 后续升级时使用，也便于联调抓包观察。
        body.put("platformMetadataFilter", decision == null ? null : decision.getMetadataFilter());
        body.put("platformFilterMode", "milvus_scope_prefilter_and_platform_second_filter");

        Object response = restTemplate.postForObject(ragServerUrl + "/rag/search", body, Object.class);

        if (!(response instanceof Map))
        {
            return results;
        }

        Map<String, Object> outer = (Map<String, Object>) response;
        Object dataObj = outer.get("data");

        Object listObj = null;

        if (dataObj instanceof List)
        {
            listObj = dataObj;
        }

        if (dataObj instanceof Map)
        {
            Map<String, Object> data = (Map<String, Object>) dataObj;
            listObj = firstList(data, "results", "records", "documents", "chunks", "list");
        }

        if (!(listObj instanceof List))
        {
            return results;
        }

        List<Object> rawList = (List<Object>) listObj;

        for (Object itemObj : rawList)
        {
            if (!(itemObj instanceof Map))
            {
                continue;
            }

            Map<String, Object> item = (Map<String, Object>) itemObj;

            RagSearchResult result = new RagSearchResult();
            result.setDocId(firstNotEmpty(item, "docId", "doc_id", "fileId", "file_id"));
            result.setTitle(firstNotEmpty(item, "title", "fileName", "file_name"));
            result.setContent(firstNotEmpty(item, "content", "summary", "text", "chunkContent", "chunk_content"));
            result.setScopeCode(firstNotEmpty(item, "scopeCode", "scope_code"));
            result.setLevel(firstNotEmpty(item, "level", "securityLevel", "security_level"));
            result.setOwnerGroupCode(firstNotEmpty(item, "ownerGroupCode", "owner_group_code", "groupId", "group_id"));
            result.setOwnerGroupName(firstNotEmpty(item, "ownerGroupName", "owner_group_name", "groupName", "group_name"));
            result.setMetadataStatus(firstNotEmpty(item, "metadataStatus", "metadata_status"));

            // RAG Server 返回 chunkId / score 时，先合并到内容里，方便前端和审计观察。
            String chunkId = firstNotEmpty(item, "chunkId", "chunk_id");
            String score = firstNotEmpty(item, "score", "distance");
            if (chunkId != null || score != null)
            {
                String content = result.getContent() == null ? "" : result.getContent();
                StringBuilder builder = new StringBuilder();
                builder.append(content);
                builder.append("\n\n[RemoteRagMeta]");
                if (chunkId != null)
                {
                    builder.append(" chunkId=").append(chunkId);
                }
                if (score != null)
                {
                    builder.append(" score=").append(score);
                }
                result.setContent(builder.toString());
            }

            result.setPassed(false);
            result.setFilterReason("");

            results.add(result);
        }

        return results;
    }

    /**
     * 构建 fufu RAG Server 当前 Milvus collection 可识别的过滤表达式。
     */
    private String buildMilvusCompatibleFilter(PermissionContext context)
    {
        if (context == null || CollectionUtils.isEmpty(context.getScopeCodes()))
        {
            return "chunk_id != \"\"";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("scope_code in [");

        List<String> scopes = context.getScopeCodes();
        for (int i = 0; i < scopes.size(); i++)
        {
            if (i > 0)
            {
                builder.append(", ");
            }
            builder.append("\"");
            builder.append(scopes.get(i));
            builder.append("\"");
        }

        builder.append("]");
        return builder.toString();
    }

    private Object firstList(Map<String, Object> map, String... keys)
    {
        if (map == null || keys == null)
        {
            return null;
        }

        for (String key : keys)
        {
            Object value = map.get(key);
            if (value instanceof List)
            {
                return value;
            }
        }

        return null;
    }

    private String firstNotEmpty(Map<String, Object> map, String... keys)
    {
        if (map == null || keys == null)
        {
            return null;
        }

        for (String key : keys)
        {
            Object value = map.get(key);
            if (value != null && String.valueOf(value).length() > 0)
            {
                return String.valueOf(value);
            }
        }

        return null;
    }
}
