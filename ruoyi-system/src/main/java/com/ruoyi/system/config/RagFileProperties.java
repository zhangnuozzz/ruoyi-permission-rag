package com.ruoyi.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG文件处理配置
 *
 * @author fufu
 * @date 2026-05-05
 */
@Component
@ConfigurationProperties(prefix = "rag.file")
public class RagFileProperties
{
    private int chunkSize = 1000;
    private int chunkOverlap = 100;
    private Minio minio = new Minio();
    private Milvus milvus = new Milvus();

    public int getChunkSize()
    {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize)
    {
        this.chunkSize = chunkSize;
    }

    public int getChunkOverlap()
    {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap)
    {
        this.chunkOverlap = chunkOverlap;
    }

    public Minio getMinio()
    {
        return minio;
    }

    public void setMinio(Minio minio)
    {
        this.minio = minio;
    }

    public Milvus getMilvus()
    {
        return milvus;
    }

    public void setMilvus(Milvus milvus)
    {
        this.milvus = milvus;
    }

    public static class Minio
    {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private String bucket = "rag-files";

        public String getEndpoint()
        {
            return endpoint;
        }

        public void setEndpoint(String endpoint)
        {
            this.endpoint = endpoint;
        }

        public String getAccessKey()
        {
            return accessKey;
        }

        public void setAccessKey(String accessKey)
        {
            this.accessKey = accessKey;
        }

        public String getSecretKey()
        {
            return secretKey;
        }

        public void setSecretKey(String secretKey)
        {
            this.secretKey = secretKey;
        }

        public String getBucket()
        {
            return bucket;
        }

        public void setBucket(String bucket)
        {
            this.bucket = bucket;
        }
    }

    public static class Milvus
    {
        private String host = "localhost";
        private int port = 19530;
        private String collectionName = "rag_file_chunks";
        private int vectorDimension = 128;

        public String getHost()
        {
            return host;
        }

        public void setHost(String host)
        {
            this.host = host;
        }

        public int getPort()
        {
            return port;
        }

        public void setPort(int port)
        {
            this.port = port;
        }

        public String getCollectionName()
        {
            return collectionName;
        }

        public void setCollectionName(String collectionName)
        {
            this.collectionName = collectionName;
        }

        public int getVectorDimension()
        {
            return vectorDimension;
        }

        public void setVectorDimension(int vectorDimension)
        {
            this.vectorDimension = vectorDimension;
        }
    }
}
