package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.rag.RagSearchResult;
import com.ruoyi.system.service.IRagAnswerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RagAnswerServiceImpl implements IRagAnswerService
{
    @Value("${rag.answer.enabled:${RAG_ANSWER_ENABLED:false}}")
    private Boolean enabled;

    @Value("${rag.answer.base-url:${RAG_ANSWER_BASE_URL:http://localhost:4000/v1}}")
    private String baseUrl;

    @Value("${rag.answer.api-key:${RAG_ANSWER_API_KEY:demo-key}}")
    private String apiKey;

    @Value("${rag.answer.model:${RAG_ANSWER_MODEL:qwen-demo}}")
    private String model;

    @Value("${rag.answer.temperature:${RAG_ANSWER_TEMPERATURE:0.2}}")
    private Double temperature;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean isEnabled()
    {
        return Boolean.TRUE.equals(enabled);
    }

    @Override
    public String getModelName()
    {
        return model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String generateAnswer(String query, List<RagSearchResult> filteredResults)
    {
        if (!isEnabled())
        {
            return "外部模型回答未启用，当前仅展示授权检索片段。";
        }

        if (filteredResults == null || filteredResults.isEmpty())
        {
            return "未检索到当前用户有权限查看的文档片段，无法生成回答。";
        }

        String contextText = buildAuthorizedContext(filteredResults);

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", model);
        body.put("temperature", temperature);

        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();

        Map<String, String> systemMessage = new LinkedHashMap<String, String>();
        systemMessage.put("role", "system");
        systemMessage.put("content",
            "你是一个企业知识库 RAG 助手。你只能依据已经通过权限过滤的授权文档片段回答。"
                + "如果授权片段中没有答案，就明确说明未在授权文档中找到依据。"
                + "不要编造未出现在授权片段中的信息。");
        messages.add(systemMessage);

        Map<String, String> userMessage = new LinkedHashMap<String, String>();
        userMessage.put("role", "user");
        userMessage.put("content",
            "用户问题：\n" + safe(query) + "\n\n"
                + "已授权文档片段：\n" + contextText + "\n\n"
                + "请基于以上授权片段给出简洁回答。");
        messages.add(userMessage);

        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && apiKey.trim().length() > 0)
        {
            headers.set("Authorization", "Bearer " + apiKey.trim());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(body, headers);

        Object response = restTemplate.postForObject(normalizeBaseUrl(baseUrl) + "/chat/completions", entity, Object.class);

        if (!(response instanceof Map))
        {
            return "外部模型返回格式异常，未能生成回答。";
        }

        Map<String, Object> responseMap = (Map<String, Object>) response;
        Object choicesObj = responseMap.get("choices");

        if (!(choicesObj instanceof List) || ((List<?>) choicesObj).isEmpty())
        {
            return "外部模型未返回 choices，未能生成回答。";
        }

        Object firstChoiceObj = ((List<?>) choicesObj).get(0);
        if (!(firstChoiceObj instanceof Map))
        {
            return "外部模型 choices 格式异常，未能生成回答。";
        }

        Map<String, Object> firstChoice = (Map<String, Object>) firstChoiceObj;
        Object messageObj = firstChoice.get("message");

        if (messageObj instanceof Map)
        {
            Object content = ((Map<String, Object>) messageObj).get("content");
            if (content != null)
            {
                return String.valueOf(content);
            }
        }

        Object text = firstChoice.get("text");
        if (text != null)
        {
            return String.valueOf(text);
        }

        return "外部模型返回内容为空，未能生成回答。";
    }

    private String buildAuthorizedContext(List<RagSearchResult> filteredResults)
    {
        StringBuilder builder = new StringBuilder();

        int index = 1;
        for (RagSearchResult item : filteredResults)
        {
            if (item == null)
            {
                continue;
            }

            builder.append(index).append(". ");
            builder.append("docId=").append(safe(item.getDocId())).append("；");
            builder.append("scopeCode=").append(safe(item.getScopeCode())).append("；");
            builder.append("level=").append(safe(item.getLevel())).append("\n");
            builder.append(safe(item.getContent())).append("\n\n");

            index++;
        }

        return builder.toString();
    }

    private String normalizeBaseUrl(String url)
    {
        if (url == null || url.trim().length() == 0)
        {
            return "http://localhost:4000/v1";
        }

        String value = url.trim();
        while (value.endsWith("/"))
        {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String safe(String value)
    {
        return value == null ? "" : value;
    }
}
