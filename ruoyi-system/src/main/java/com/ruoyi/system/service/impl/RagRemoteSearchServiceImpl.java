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
 * RAG Server 远程真实检索 Service 实现
 *
 * 当前按照 docs/RAG检索联调接口约定.md 调用 RAG Server：
 * POST {rag.server.url}/rag/search
 *
 * 注意：fufu_week3 当前尚未实现该接口，本类用于平台侧提前完成适配层。
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

        Map<String, Object> userContext = new LinkedHashMap<String, Object>();
        userContext.put("userId", context.getUserId());
        userContext.put("userName", context.getUserName());
        userContext.put("admin", context.getAdmin());
        userContext.put("groupCodes", context.getGroupCodes());
        userContext.put("scopeCodes", context.getScopeCodes());

        body.put("userContext", userContext);
        body.put("metadataFilter", decision == null ? null : decision.getMetadataFilter());
        body.put("platformFilterMode", "metadata_filter_and_second_filter");

        Object response = restTemplate.postForObject(ragServerUrl + "/rag/search", body, Object.class);

        if (!(response instanceof Map))
        {
            return results;
        }

        Map<String, Object> outer = (Map<String, Object>) response;
        Object dataObj = outer.get("data");

        if (!(dataObj instanceof Map))
        {
            return results;
        }

        Map<String, Object> data = (Map<String, Object>) dataObj;
        Object listObj = data.get("results");

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
            result.setContent(firstNotEmpty(item, "content", "text", "chunkContent", "chunk_content"));
            result.setScopeCode(firstNotEmpty(item, "scopeCode", "scope_code"));
            result.setLevel(firstNotEmpty(item, "level", "securityLevel", "security_level"));
            result.setPassed(false);
            result.setFilterReason("");

            results.add(result);
        }

        return results;
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
