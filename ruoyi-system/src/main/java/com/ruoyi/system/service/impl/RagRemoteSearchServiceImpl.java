package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.permission.PolicyDecisionResult;
import com.ruoyi.system.domain.rag.RagSearchRequest;
import com.ruoyi.system.domain.rag.RagSearchResult;
import com.ruoyi.system.service.IRagRemoteSearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将真实检索结果转换为平台文档片段。
 * 检索前传递密级与知悉组过滤条件，检索后仍需执行平台文档鉴权。
 * 片段正文与诊断字段分别保存，避免诊断信息进入问答依据。
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

        /*
         * Milvus 前置过滤使用统一安全过滤表达式：
         *
         * PUBLIC：
         * 所有人；
         *
         * INTERNAL：
         * 根据人员密级；
         *
         * SECRET / CONFIDENTIAL：
         * 根据人员密级 + group_id。
         *
         * 不再使用 scope_code 作为统一权限过滤条件。
         */
        String metadataFilter =
                decision == null
                        || decision.getMetadataFilter() == null
                        || decision.getMetadataFilter().length() == 0
                        ? "chunk_id != \"\""
                        : decision.getMetadataFilter();

        body.put(
                "metadataFilter",
                metadataFilter
        );

        body.put(
                "platformMetadataFilter",
                metadataFilter
        );

        body.put(
                "platformFilterMode",
                "security_level_and_knowledge_group_prefilter"
        );

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

            // 诊断字段独立存储，不能拼入授权正文或模型输入。
            result.setChunkId(firstNotEmpty(item, "chunkId", "chunk_id"));
            result.setScore(firstNotEmpty(item, "score", "distance"));

            result.setPassed(false);
            result.setFilterReason("");

            results.add(result);
        }

        return results;
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
