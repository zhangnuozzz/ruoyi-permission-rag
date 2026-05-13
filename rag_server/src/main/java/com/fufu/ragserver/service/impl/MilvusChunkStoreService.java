package com.fufu.ragserver.service.impl;

import com.fufu.ragserver.config.RagFileProperties;
import com.fufu.ragserver.domain.RagFileChunk;
import com.fufu.ragserver.exception.ServiceException;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.QueryResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.CollectionSchemaParam;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.index.DescribeIndexParam;
import io.milvus.response.QueryResultsWrapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

/**
 * Milvus文件切块存储服务
 *
 * @author fufu
 * @date 2026-05-13 12:37:47 CST
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
    private final AtomicBoolean collectionReady = new AtomicBoolean(false);

    public MilvusChunkStoreService(RagFileProperties properties)
    {
        this.properties = properties;
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
            R<MutationResult> result = client.insert(InsertParam.newBuilder()
                .withCollectionName(properties.getMilvus().getCollectionName())
                .withFields(buildFields(chunks))
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

    public List<Map<String, Object>> listChunks(int limit)
    {
        MilvusServiceClient client = newClient();
        try
        {
            ensureCollection(client);
            client.loadCollection(LoadCollectionParam.newBuilder()
                .withCollectionName(properties.getMilvus().getCollectionName())
                .build());
            R<QueryResults> result = queryChunks(client, limit, Arrays.asList(FIELD_CHUNK_ID, FIELD_CONTENT,
                FIELD_FILE_ID, FIELD_SECURITY_LEVEL, FIELD_SCOPE_CODE, FIELD_GROUP_ID, FIELD_GROUP_NAME));
            if (result.getStatus() != R.Status.Success.getCode() && result.getMessage() != null
                && result.getMessage().contains(FIELD_SCOPE_CODE))
            {
                result = queryChunks(client, limit, Arrays.asList(FIELD_CHUNK_ID, FIELD_CONTENT, FIELD_FILE_ID,
                    FIELD_SECURITY_LEVEL, FIELD_GROUP_ID, FIELD_GROUP_NAME));
            }
            checkQueryResult(result);
            QueryResultsWrapper wrapper = new QueryResultsWrapper(result.getData());
            return toRows(wrapper, Math.max(1, limit));
        }
        finally
        {
            closeClient(client);
        }
    }

    private R<QueryResults> queryChunks(MilvusServiceClient client, int limit, List<String> outFields)
    {
        return client.query(QueryParam.newBuilder()
            .withCollectionName(properties.getMilvus().getCollectionName())
            .withOutFields(outFields)
            .withExpr(FIELD_CHUNK_ID + " != \"\"")
            .withLimit(Long.valueOf(Math.max(1, limit)))
            .build());
    }

    private void checkQueryResult(R<QueryResults> result)
    {
        if (result.getStatus() != R.Status.Success.getCode())
        {
            throw new ServiceException("Milvus查询失败：" + result.getMessage());
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

    private List<InsertParam.Field> buildFields(List<RagFileChunk> chunks)
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

        return Arrays.asList(
            new InsertParam.Field(FIELD_CHUNK_ID, chunkIds),
            new InsertParam.Field(FIELD_CONTENT, contents),
            new InsertParam.Field(FIELD_FILE_ID, fileIds),
            new InsertParam.Field(FIELD_SECURITY_LEVEL, securityLevels),
            new InsertParam.Field(FIELD_SCOPE_CODE, scopeCodes),
            new InsertParam.Field(FIELD_GROUP_ID, groupIds),
            new InsertParam.Field(FIELD_GROUP_NAME, groupNames),
            new InsertParam.Field(FIELD_EMBEDDING, embeddings));
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

    private List<Map<String, Object>> toRows(QueryResultsWrapper wrapper, int limit)
    {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < limit; i++)
        {
            Map<String, Object> row = new LinkedHashMap<>();
            putField(row, wrapper, FIELD_CHUNK_ID, i);
            if (row.isEmpty())
            {
                break;
            }
            putField(row, wrapper, FIELD_FILE_ID, i);
            putField(row, wrapper, FIELD_SECURITY_LEVEL, i);
            putField(row, wrapper, FIELD_SCOPE_CODE, i);
            putField(row, wrapper, FIELD_GROUP_ID, i);
            putField(row, wrapper, FIELD_GROUP_NAME, i);
            putField(row, wrapper, FIELD_CONTENT, i);
            rows.add(row);
        }
        return rows;
    }

    private void putField(Map<String, Object> row, QueryResultsWrapper wrapper, String fieldName, int index)
    {
        try
        {
            List<?> values = wrapper.getFieldWrapper(fieldName).getFieldData();
            if (index < values.size())
            {
                row.put(fieldName, values.get(index));
            }
        }
        catch (RuntimeException ignored)
        {
            // Milvus may omit an optional field when the collection is empty.
        }
    }
}
