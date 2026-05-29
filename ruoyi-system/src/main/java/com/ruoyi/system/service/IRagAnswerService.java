package com.ruoyi.system.service;

import com.ruoyi.system.domain.rag.RagSearchResult;

import java.util.List;

/**
 * RAG 外部模型回答服务
 *
 * 只允许使用平台侧二次过滤后的授权片段生成回答。
 */
public interface IRagAnswerService
{
    boolean isEnabled();

    String getModelName();

    String generateAnswer(String query, List<RagSearchResult> filteredResults);
}
