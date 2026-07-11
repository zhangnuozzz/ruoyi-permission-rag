package com.fufu.ragserver.service.impl;

import com.fufu.ragserver.config.RagFileProperties;
import com.fufu.ragserver.domain.RagFileChunk;
import com.fufu.ragserver.domain.RagSearchRequest;
import com.fufu.ragserver.domain.RagSearchResult;
import com.fufu.ragserver.domain.SysRagFile;
import com.fufu.ragserver.exception.ServiceException;
import com.fufu.ragserver.mapper.SysRagFileMapper;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.DescribeCollectionResponse;
import io.milvus.grpc.FieldSchema;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.CollectionSchemaParam;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.DescribeCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.index.DescribeIndexParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Milvus文件切块存储服务
 *
 * @author fufu
 * @date 2026-05-12
 */
@Service
public class MilvusChunkStoreService
{
    private static final String FIELD_CHUNK_ID = "chunk_id";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_FILE_ID = "file_id";
    private static final String FIELD_SECURITY_LEVEL = "security_level";
    private static final String FIELD_SCOPE_CODE = "scope_code";
    private static final String FIELD_GROUP_ID = "group_id";
    private static final String FIELD_GROUP_NAME = "group_name";
    private static final String FIELD_EMBEDDING = "embedding";

    private final RagFileProperties properties;
    private final SysRagFileMapper sysRagFileMapper;
    private final AtomicBoolean collectionReady = new AtomicBoolean(false);

    public MilvusChunkStoreService(RagFileProperties properties, SysRagFileMapper sysRagFileMapper)
    {
        this.properties = properties;
        this.sysRagFileMapper = sysRagFileMapper;
    }

    public void saveChunks(List<RagFileChunk> chunks)
    {
        if (chunks.isEmpty())
        {
            return;
        }

        MilvusServiceClient client = newClient();
        try
        {
            ensureCollection(client);
            Set<String> fieldNames = describeFieldNames(client);
            R<MutationResult> result = client.insert(InsertParam.newBuilder()
                .withCollectionName(properties.getMilvus().getCollectionName())
                .withFields(buildFields(chunks, fieldNames))
                .build());
            if (result.getStatus() != R.Status.Success.getCode())
            {
                throw new ServiceException("Milvus写入失败：" + result.getMessage());
            }
        }
        finally
        {
            closeClient(client);
        }
    }

    public List<RagSearchResult> search(RagSearchRequest request)
    {
        if (request == null || StringUtils.isBlank(request.getQuery()))
        {
            throw new ServiceException("查询内容不能为空");
        }

        MilvusServiceClient client = newClient();
        try
        {
            ensureCollection(client);
            Set<String> fieldNames = describeFieldNames(client);
            validateSearchFilterFields(request.getMetadataFilter(), fieldNames);
            int topK = request.getTopK() == null || request.getTopK() <= 0 ? 5 : request.getTopK();
            R<SearchResults> result = client.search(SearchParam.newBuilder()
                .withCollectionName(properties.getMilvus().getCollectionName())
                .withMetricType(MetricType.L2)
                .withVectorFieldName(FIELD_EMBEDDING)
                .withTopK(topK)
                .withExpr(request.getMetadataFilter())
                .withFloatVectors(Arrays.asList(textVector(request.getQuery())))
                .withOutFields(availableOutFields(fieldNames))
                .build());
            if (result.getStatus() != R.Status.Success.getCode())
            {
                throw new ServiceException("Milvus检索失败：" + result.getMessage());
            }
            return toSearchResults(result.getData());
        }
        finally
        {
            closeClient(client);
        }
    }

    public List<Map<String, Object>> listChunks(Integer limit)
    {
        MilvusServiceClient client = newClient();
        try
        {
            ensureCollection(client);
            Set<String> fieldNames = describeFieldNames(client);
            R<io.milvus.grpc.QueryResults> result = client.query(QueryParam.newBuilder()
                .withCollectionName(properties.getMilvus().getCollectionName())
                .withExpr(FIELD_CHUNK_ID + " != \"\"")
                .withOutFields(availableOutFields(fieldNames))
                .withLimit(limit == null || limit <= 0 ? 100L : limit.longValue())
                .build());
            if (result.getStatus() != R.Status.Success.getCode())
            {
                throw new ServiceException("Milvus切块查询失败：" + result.getMessage());
            }

            QueryResultsWrapper wrapper = new QueryResultsWrapper(result.getData());
            List<Map<String, Object>> chunks = new ArrayList<>();
            for (QueryResultsWrapper.RowRecord row : wrapper.getRowRecords())
            {
                Map<String, Object> values = new LinkedHashMap<>(row.getFieldValues());
                enrichFileMetadata(values);
                chunks.add(values);
            }
            return chunks;
        }
        finally
        {
            closeClient(client);
        }
    }

    private MilvusServiceClient newClient()
    {
        RagFileProperties.Milvus milvus = properties.getMilvus();
        return new MilvusServiceClient(ConnectParam.newBuilder()
            .withHost(milvus.getHost())
            .withPort(milvus.getPort())
            .build());
    }

    private void closeClient(MilvusServiceClient client)
    {
        try
        {
            client.close(5);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private void ensureCollection(MilvusServiceClient client)
    {
        if (collectionReady.get())
        {
            return;
        }

        String collectionName = properties.getMilvus().getCollectionName();
        R<Boolean> exists = client.hasCollection(HasCollectionParam.newBuilder()
            .withCollectionName(collectionName)
            .build());
        if (exists.getStatus() != R.Status.Success.getCode())
        {
            throw new ServiceException("Milvus集合检查失败：" + exists.getMessage());
        }
        if (!Boolean.TRUE.equals(exists.getData()))
        {
            R<RpcStatus> created = client.createCollection(CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withSchema(CollectionSchemaParam.newBuilder()
                    .withEnableDynamicField(false)
                    .withFieldTypes(buildSchemaFields())
                    .build())
                .build());
            if (created.getStatus() != R.Status.Success.getCode())
            {
                throw new ServiceException("Milvus集合创建失败：" + created.getMessage());
            }
        }
        ensureVectorIndex(client, collectionName);
        R<RpcStatus> loaded = client.loadCollection(LoadCollectionParam.newBuilder()
            .withCollectionName(collectionName)
            .withSyncLoad(true)
            .build());
        if (loaded.getStatus() != R.Status.Success.getCode())
        {
            throw new ServiceException("Milvus集合加载失败：" + loaded.getMessage());
        }
        collectionReady.set(true);
    }

    private void ensureVectorIndex(MilvusServiceClient client, String collectionName)
    {
        R<?> index = client.describeIndex(DescribeIndexParam.newBuilder()
            .withCollectionName(collectionName)
            .withFieldName(FIELD_EMBEDDING)
            .build());
        if (index.getStatus() == R.Status.Success.getCode())
        {
            return;
        }

        R<RpcStatus> created = client.createIndex(CreateIndexParam.newBuilder()
            .withCollectionName(collectionName)
            .withFieldName(FIELD_EMBEDDING)
            .withIndexType(IndexType.AUTOINDEX)
            .withMetricType(MetricType.L2)
            .withSyncMode(Boolean.TRUE)
            .build());
        if (created.getStatus() != R.Status.Success.getCode())
        {
            throw new ServiceException("Milvus向量索引创建失败：" + created.getMessage());
        }
    }

    private List<FieldType> buildSchemaFields()
    {
        return Arrays.asList(
            FieldType.newBuilder()
                .withName(FIELD_CHUNK_ID)
                .withDataType(DataType.VarChar)
                .withPrimaryKey(true)
                .withAutoID(false)
                .withMaxLength(128)
                .build(),
            varcharField(FIELD_CONTENT, 65535),
            varcharField(FIELD_FILE_ID, 64),
            varcharField(FIELD_SECURITY_LEVEL, 64),
            varcharField(FIELD_SCOPE_CODE, 64),
            varcharField(FIELD_GROUP_ID, 64),
            varcharField(FIELD_GROUP_NAME, 128),
            FieldType.newBuilder()
                .withName(FIELD_EMBEDDING)
                .withDataType(DataType.FloatVector)
                .withDimension(properties.getMilvus().getVectorDimension())
                .build());
    }

    private FieldType varcharField(String name, int maxLength)
    {
        return FieldType.newBuilder()
            .withName(name)
            .withDataType(DataType.VarChar)
            .withMaxLength(maxLength)
            .build();
    }

    private Set<String> describeFieldNames(MilvusServiceClient client)
    {
        R<DescribeCollectionResponse> response = client.describeCollection(DescribeCollectionParam.newBuilder()
            .withCollectionName(properties.getMilvus().getCollectionName())
            .build());
        if (response.getStatus() != R.Status.Success.getCode())
        {
            throw new ServiceException("Milvus集合结构读取失败：" + response.getMessage());
        }

        Set<String> fieldNames = new HashSet<>();
        for (FieldSchema field : response.getData().getSchema().getFieldsList())
        {
            fieldNames.add(field.getName());
        }
        return fieldNames;
    }

    private List<String> availableOutFields(Set<String> fieldNames)
    {
        List<String> fields = new ArrayList<>();
        addIfPresent(fields, fieldNames, FIELD_CHUNK_ID);
        addIfPresent(fields, fieldNames, FIELD_CONTENT);
        addIfPresent(fields, fieldNames, FIELD_FILE_ID);
        addIfPresent(fields, fieldNames, FIELD_SECURITY_LEVEL);
        addIfPresent(fields, fieldNames, FIELD_SCOPE_CODE);
        addIfPresent(fields, fieldNames, FIELD_GROUP_ID);
        addIfPresent(fields, fieldNames, FIELD_GROUP_NAME);
        return fields;
    }

    private void addIfPresent(List<String> fields, Set<String> fieldNames, String fieldName)
    {
        if (fieldNames.contains(fieldName))
        {
            fields.add(fieldName);
        }
    }

    private void validateSearchFilterFields(String metadataFilter, Set<String> fieldNames)
    {
        if (StringUtils.isBlank(metadataFilter))
        {
            return;
        }
        if (metadataFilter.contains(FIELD_SCOPE_CODE) && !fieldNames.contains(FIELD_SCOPE_CODE))
        {
            throw new ServiceException("Milvus集合缺少权限字段scope_code，请重建集合或切换新的collection-name后重新上传文档");
        }
        if (metadataFilter.contains(FIELD_SECURITY_LEVEL) && !fieldNames.contains(FIELD_SECURITY_LEVEL))
        {
            throw new ServiceException("Milvus集合缺少权限字段security_level，请重建集合或切换新的collection-name后重新上传文档");
        }
    }

    private List<InsertParam.Field> buildFields(List<RagFileChunk> chunks, Set<String> fieldNames)
    {
        List<String> chunkIds = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        List<String> fileIds = new ArrayList<>();
        List<String> securityLevels = new ArrayList<>();
        List<String> scopeCodes = new ArrayList<>();
        List<String> groupIds = new ArrayList<>();
        List<String> groupNames = new ArrayList<>();
        List<List<Float>> embeddings = new ArrayList<>();

        for (RagFileChunk chunk : chunks)
        {
            chunkIds.add(chunk.getChunkId());
            contents.add(trimToMilvusVarchar(chunk.getContent(), 65535));
            fileIds.add(chunk.getFileId());
            securityLevels.add(chunk.getSecurityLevel());
            scopeCodes.add(chunk.getScopeCode());
            groupIds.add(chunk.getGroupId());
            groupNames.add(chunk.getGroupName());
            embeddings.add(textVector(chunk.getContent()));
        }

        List<InsertParam.Field> fields = new ArrayList<>();
        addInsertField(fields, fieldNames, FIELD_CHUNK_ID, chunkIds);
        addInsertField(fields, fieldNames, FIELD_CONTENT, contents);
        addInsertField(fields, fieldNames, FIELD_FILE_ID, fileIds);
        addInsertField(fields, fieldNames, FIELD_SECURITY_LEVEL, securityLevels);
        addInsertField(fields, fieldNames, FIELD_SCOPE_CODE, scopeCodes);
        addInsertField(fields, fieldNames, FIELD_GROUP_ID, groupIds);
        addInsertField(fields, fieldNames, FIELD_GROUP_NAME, groupNames);
        addInsertField(fields, fieldNames, FIELD_EMBEDDING, embeddings);
        return fields;
    }

    private void addInsertField(List<InsertParam.Field> fields, Set<String> fieldNames, String fieldName, List<?> values)
    {
        if (fieldNames.contains(fieldName))
        {
            fields.add(new InsertParam.Field(fieldName, values));
        }
    }

    private void enrichFileMetadata(Map<String, Object> values)
    {
        Object fileId = values.get(FIELD_FILE_ID);
        if (fileId == null)
        {
            putMissing(values, FIELD_SECURITY_LEVEL, "");
            putMissing(values, FIELD_SCOPE_CODE, "");
            putMissing(values, FIELD_GROUP_ID, "");
            putMissing(values, FIELD_GROUP_NAME, "");
            return;
        }

        SysRagFile file = sysRagFileMapper.selectSysRagFileByFileId(String.valueOf(fileId));
        if (file == null)
        {
            putMissing(values, FIELD_SECURITY_LEVEL, "");
            putMissing(values, FIELD_SCOPE_CODE, "");
            putMissing(values, FIELD_GROUP_ID, "");
            putMissing(values, FIELD_GROUP_NAME, "");
            return;
        }
        putMissing(values, FIELD_SECURITY_LEVEL, file.getSecurityLevel());
        putMissing(values, FIELD_SCOPE_CODE, file.getScopeCode());
        putMissing(values, FIELD_GROUP_ID, file.getGroupId());
        putMissing(values, FIELD_GROUP_NAME, file.getGroupName());
    }

    private void putMissing(Map<String, Object> values, String key, Object value)
    {
        if (!values.containsKey(key) || values.get(key) == null)
        {
            values.put(key, value == null ? "" : value);
        }
    }

    private String trimToMilvusVarchar(String value, int maxLength)
    {
        if (value == null || value.length() <= maxLength)
        {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private List<Float> textVector(String text)
    {
        int dimension = properties.getMilvus().getVectorDimension();
        float[] vector = new float[dimension];
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++)
        {
            int index = i % dimension;
            vector[index] += (bytes[i] & 0xff) / 255.0F;
        }
        List<Float> values = new ArrayList<>(dimension);
        for (float value : vector)
        {
            values.add(value);
        }
        return values;
    }

    private List<RagSearchResult> toSearchResults(SearchResults data)
    {
        List<RagSearchResult> results = new ArrayList<>();
        SearchResultsWrapper wrapper = new SearchResultsWrapper(data.getResults());
        List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
        for (SearchResultsWrapper.IDScore score : scores)
        {
            RagSearchResult result = new RagSearchResult();
            result.setChunkId(valueAsString(score, FIELD_CHUNK_ID, score.getStrID()));
            result.setDocId(valueAsString(score, FIELD_FILE_ID, ""));
            result.setTitle(result.getDocId());
            result.setContent(valueAsString(score, FIELD_CONTENT, ""));
            result.setSummary(buildSummary(result.getContent()));
            result.setScopeCode(valueAsString(score, FIELD_SCOPE_CODE, ""));
            result.setLevel(valueAsString(score, FIELD_SECURITY_LEVEL, ""));
            result.setScore(score.getScore());
            result.setPassed(false);
            result.setFilterReason("");
            results.add(result);
        }
        return results;
    }

    private String valueAsString(SearchResultsWrapper.IDScore score, String key, String defaultValue)
    {
        Object value = score.getFieldValues().get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private String buildSummary(String content)
    {
        if (content == null)
        {
            return "";
        }
        String text = content.replaceAll("\\s+", " ").trim();
        return text.length() <= 160 ? text : text.substring(0, 160);
    }
}
