package com.ruoyi.system.service.impl;

import java.io.ByteArrayInputStream;
import org.springframework.stereotype.Service;
import com.ruoyi.system.config.RagFileProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

/**
 * MinIO原始文件备份服务
 *
 * @author fufu
 * @date 2026-05-05
 */
@Service
public class MinioFileBackupService
{
    private final RagFileProperties properties;

    public MinioFileBackupService(RagFileProperties properties)
    {
        this.properties = properties;
    }

    public String backup(byte[] content, String objectName, String contentType) throws Exception
    {
        RagFileProperties.Minio minioProperties = properties.getMinio();
        MinioClient client = MinioClient.builder()
            .endpoint(minioProperties.getEndpoint())
            .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
            .build();
        ensureBucket(client, minioProperties.getBucket());

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content))
        {
            client.putObject(PutObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(objectName)
                .stream(inputStream, content.length, -1)
                .contentType(contentType)
                .build());
        }
        return objectName;
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
