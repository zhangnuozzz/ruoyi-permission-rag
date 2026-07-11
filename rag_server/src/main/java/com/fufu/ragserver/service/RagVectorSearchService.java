package com.fufu.ragserver.service;

import com.fufu.ragserver.domain.RagSearchRequest;
import com.fufu.ragserver.domain.RagSearchResult;
import java.util.List;

public interface RagVectorSearchService
{
    List<RagSearchResult> search(RagSearchRequest request);
}
