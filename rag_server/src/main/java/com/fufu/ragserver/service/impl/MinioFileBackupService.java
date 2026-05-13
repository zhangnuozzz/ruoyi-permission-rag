package com.fufu.ragserver.service.impl;

import com.fufu.ragserver.config.RagFileProperties;
import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * MinIO原始文件备份服务
 *
 * @author fufu
 * @date 2026-05-13 12:37:47 CST
 */
@Service
public class MinioFileBackupService
{
    private final RagFileProperties properties;

    public MinioFileBackupService(RagFileProperties properties)
    {
        this.properties = properties;
    }

    public void backup(byte[] content, String objectName, String contentType) throws Exception
    {
        RagFileProperties.Minio minioProperties = properties.getMinio();
        MinioClient client = MinioClient.builder()
            .endpoint(minioProperties.getEndpoint())
            .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
            .build();

        ensureBucket(client, minioProperties.getBucket());
        client.putObject(PutObjectArgs.builder()
            .bucket(minioProperties.getBucket())
            .object(objectName)
            .stream(new ByteArrayInputStream(content), content.length, -1)
            .contentType(contentType)
            .build());
    }

    public List<Map<String, Object>> listObjects(int limit) throws Exception
    {
        RagFileProperties.Minio minioProperties = properties.getMinio();
        MinioClient client = MinioClient.builder()
            .endpoint(minioProperties.getEndpoint())
            .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
            .build();

        ensureBucket(client, minioProperties.getBucket());
        List<Map<String, Object>> objects = new ArrayList<>();
        Iterable<Result<Item>> results = client.listObjects(ListObjectsArgs.builder()
            .bucket(minioProperties.getBucket())
            .recursive(true)
            .build());
        for (Result<Item> result : results)
        {
            if (objects.size() >= Math.max(1, limit))
            {
                break;
            }
            Item item = result.get();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("objectName", item.objectName());
            row.put("size", item.size());
            row.put("etag", item.etag());
            row.put("lastModified", item.lastModified() == null ? null : item.lastModified().toString());
            objects.add(row);
        }
        return objects;
    }

    private void ensureBucket(MinioClient client, String bucket) throws Exception
    {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists)
        {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
