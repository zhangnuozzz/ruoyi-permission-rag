package com.ruoyi.system.domain.rag;

/** 检索候选预算。返回数量与候选数量分别限制，候选仍须逐条鉴权。 */
public final class RagSearchLimits
{
    public static final int MAX_CANDIDATES = 100;

    private RagSearchLimits()
    {
    }

    public static int candidateLimit(Integer topK)
    {
        int requested = topK == null || topK <= 0 ? 5 : Math.min(topK, 20);
        return Math.min(MAX_CANDIDATES, Math.max(20, requested * 5));
    }
}
