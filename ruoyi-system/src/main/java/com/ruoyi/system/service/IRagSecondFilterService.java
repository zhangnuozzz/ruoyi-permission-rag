package com.ruoyi.system.service;

import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.rag.RagSearchResult;

import java.util.List;

/**
 * RAG 检索结果二次过滤 Service
 *
 * 用于在向量数据库返回结果后，再按用户权限上下文进行应用层校验。
 */
public interface IRagSecondFilterService
{
    /**
     * 根据权限上下文对检索结果进行二次过滤。
     *
     * @param context 权限上下文
     * @param results 原始检索结果
     * @return 过滤后的检索结果
     */
    List<RagSearchResult> filter(PermissionContext context, List<RagSearchResult> results);
}
