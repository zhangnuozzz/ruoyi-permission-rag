package com.fufu.ragserver.controller;

import com.fufu.ragserver.domain.AjaxResult;
import com.fufu.ragserver.domain.RagSearchRequest;
import com.fufu.ragserver.service.RagVectorSearchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG安全向量检索Controller。
 */
@RestController
@RequestMapping("/rag")
public class RagSearchController
{
    private final RagVectorSearchService ragVectorSearchService;

    public RagSearchController(RagVectorSearchService ragVectorSearchService)
    {
        this.ragVectorSearchService = ragVectorSearchService;
    }

    @PostMapping("/search")
    public AjaxResult search(@RequestBody RagSearchRequest request)
    {
        return AjaxResult.success(ragVectorSearchService.search(request));
    }
}
