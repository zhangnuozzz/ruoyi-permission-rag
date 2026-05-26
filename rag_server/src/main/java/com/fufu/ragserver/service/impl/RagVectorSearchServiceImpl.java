package com.fufu.ragserver.service.impl;

import com.fufu.ragserver.domain.RagSearchRequest;
import com.fufu.ragserver.domain.RagSearchResult;
import com.fufu.ragserver.service.RagVectorSearchService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Milvus向量检索服务。
 */
@Service
public class RagVectorSearchServiceImpl implements RagVectorSearchService
{
    private final MilvusChunkStoreService milvusChunkStoreService;

    public RagVectorSearchServiceImpl(MilvusChunkStoreService milvusChunkStoreService)
    {
        this.milvusChunkStoreService = milvusChunkStoreService;
    }

    @Override
    public List<RagSearchResult> search(RagSearchRequest request)
    {
        return milvusChunkStoreService.search(request);
    }
}
