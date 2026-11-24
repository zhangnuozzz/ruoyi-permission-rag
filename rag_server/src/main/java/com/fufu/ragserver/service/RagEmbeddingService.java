package com.fufu.ragserver.service;

import com.fufu.ragserver.config.RagFileProperties;
import com.fufu.ragserver.exception.ServiceException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/** Shared document/query embedding client. Real mode never falls back to fake vectors. */
@Service
public class RagEmbeddingService {
    private final RagFileProperties properties;
    private final RestTemplate client;
    @Value("${rag.embedding.mode:fake}") private String mode;
    @Value("${rag.embedding.base-url:http://127.0.0.1:8001/v1}") private String baseUrl;
    @Value("${rag.embedding.model:bge-small-zh-v1.5}") private String model;
    @Value("${rag.embedding.query-prefix:为这个句子生成表示以用于检索相关文章：}") private String prefix;

    public RagEmbeddingService(RagFileProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); factory.setReadTimeout(60000);
        client = new RestTemplate(factory);
    }

    public boolean realMode() {
        if (!"fake".equals(mode) && !"http".equals(mode)) throw new ServiceException("未知embedding模式");
        return "http".equals(mode);
    }

    @SuppressWarnings("unchecked")
    public List<String> split(String text) {
        Map<String,Object> result = post("/chunks", Map.of("text", text, "max_tokens", 384, "overlap", 48));
        if (!(result.get("chunks") instanceof List)) throw new ServiceException("分块服务返回无效数据");
        List<String> chunks = new ArrayList<>();
        for (Object item : (List<?>) result.get("chunks")) {
            if (!(item instanceof String) || ((String)item).isBlank()) throw new ServiceException("分块服务返回无效文本");
            chunks.add((String)item);
        }
        return chunks;
    }

    public List<Float> query(String text) {
        return embed(Collections.singletonList(realMode() ? prefix + text : text)).get(0);
    }

    @SuppressWarnings("unchecked")
    public List<List<Float>> embed(List<String> texts) {
        List<List<Float>> vectors = new ArrayList<>();
        if (!realMode()) {
            for (String text : texts) vectors.add(fake(text));
            return vectors;
        }
        for (int start = 0; start < texts.size(); start += 32) {
            List<String> batch = texts.subList(start, Math.min(start + 32, texts.size()));
            Map<String,Object> response = post("/embeddings", Map.of("model", model, "input", batch));
            if (!(response.get("data") instanceof List)) throw new ServiceException("Embedding返回无效数据");
            List<?> data = (List<?>)response.get("data");
            if (data.size() != batch.size()) throw new ServiceException("Embedding返回数量不一致");
            for (int i=0; i<data.size(); i++) {
                Map<?,?> item = (Map<?,?>)data.get(i);
                if (!(item.get("index") instanceof Number) || ((Number)item.get("index")).intValue()!=i)
                    throw new ServiceException("Embedding顺序不一致");
                List<?> raw = (List<?>)item.get("embedding");
                if (raw.size()!=properties.getMilvus().getVectorDimension()) throw new ServiceException("Embedding维度与Milvus配置不一致");
                List<Float> vector = new ArrayList<>();
                double norm=0;
                for (Object value : raw) {
                    float number = ((Number)value).floatValue();
                    if (!Float.isFinite(number)) throw new ServiceException("Embedding包含无效数值");
                    vector.add(number); norm += (double)number * number;
                }
                if (norm==0) throw new ServiceException("Embedding为空向量");
                vectors.add(vector);
            }
        }
        return vectors;
    }

    private Map<String,Object> post(String path, Map<String,Object> body) {
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String,Object> response = client.postForObject(baseUrl.replaceAll("/+$", "") + path,
            new HttpEntity<>(body, headers), Map.class);
        if (response==null) throw new ServiceException("Embedding服务返回为空");
        return response;
    }

    private List<Float> fake(String text) {
        float[] vector = new float[properties.getMilvus().getVectorDimension()];
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        for (int i=0; i<bytes.length; i++) vector[i % vector.length] += (bytes[i]&0xff)/255.0F;
        List<Float> result = new ArrayList<>();
        for(float value : vector) result.add(value);
        return result;
    }
}
