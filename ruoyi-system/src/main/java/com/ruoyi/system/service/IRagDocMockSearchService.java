package com.ruoyi.system.service;

import com.ruoyi.system.domain.rag.RagSearchResult;

import java.util.List;

/**
 * RAG 文档模拟检索 Service
 *
 * 当前阶段暂不接真实 Milvus，从 sys_rag_doc 表读取文档权限标签，
 * 模拟返回候选检索结果，用于验证权限过滤链路。
 */
public interface IRagDocMockSearchService
{
    /**
     * 根据查询内容返回模拟检索结果。
     *
     * @param query 用户查询内容
     * @return 模拟检索结果列表
     */
    List<RagSearchResult> search(String query);
}
