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
 * 负责将平台侧权限上下文、metadataFilter 转发给 RAG Server，
 * 并兼容 RAG Server 返回的两类结果格式：
 *
 * 1. data: [ ... ]
 * 2. data: { results: [ ... ] }
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

            // 为兼容 fufu RAG Server 当前接口，也将 scopeCodes 平铺传一份。
            body.put("scopeCodes", context.getScopeCodes());
        }

        body.put("metadataFilter", decision == null ? null : decision.getMetadataFilter());
        body.put("platformFilterMode", "metadata_filter_and_second_filter");

        Object response = restTemplate.postForObject(ragServerUrl + "/rag/search", body, Object.class);

        if (!(response instanceof Map))
        {
            return results;
        }

        Map<String, Object> outer = (Map<String, Object>) response;
        Object dataObj = outer.get("data");

        Object listObj = null;

        // 兼容格式一：{"data": [ ... ]}
        if (dataObj instanceof List)
        {
            listObj = dataObj;
        }

        // 兼容格式二：{"data": {"results": [ ... ]}}
        if (dataObj instanceof Map)
        {
            Map<String, Object> data = (Map<String, Object>) dataObj;
            listObj = data.get("results");
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

            // 这里先标记 false，最终是否通过由平台侧二次过滤服务统一判定。
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
