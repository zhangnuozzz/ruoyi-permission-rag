package com.ruoyi.system.service;

import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.permission.PolicyDecisionResult;
import com.ruoyi.system.domain.rag.RagSearchRequest;
import com.ruoyi.system.domain.rag.RagSearchResult;

import java.util.List;

/**
 * RAG Server 远程真实检索 Service
 */
public interface IRagRemoteSearchService
{
    /**
     * 调用 RAG Server 执行真实检索。
     *
     * @param request 检索请求
     * @param context 当前用户权限上下文
     * @param decision 策略决策结果
     * @return RAG Server 返回的候选检索结果
     */
    List<RagSearchResult> search(RagSearchRequest request, PermissionContext context, PolicyDecisionResult decision);
}
