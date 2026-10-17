package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.SysRagDoc;
import com.ruoyi.system.domain.rag.RagSearchResult;
import com.ruoyi.system.mapper.SysRagDocMapper;
import com.ruoyi.system.service.IRagDocMockSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RAG 文档模拟检索 Service 实现
 *
 * 当前版本从 sys_rag_doc 表读取文档权限标签，模拟检索候选结果。
 * 后续接入 Milvus 后，该服务可替换为真实向量检索服务。
 */
@Service
public class RagDocMockSearchServiceImpl implements IRagDocMockSearchService
{
    @Autowired
    private SysRagDocMapper sysRagDocMapper;

    @Override
    public List<RagSearchResult> search(String query)
    {
        SysRagDoc condition = new SysRagDoc();
        condition.setStatus("0");

        List<SysRagDoc> docs = sysRagDocMapper.selectSysRagDocList(condition);
        List<RagSearchResult> results = new ArrayList<RagSearchResult>();

        if (docs == null || docs.isEmpty())
        {
            return results;
        }

        String keyword = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (SysRagDoc doc : docs)
        {
            if (doc == null || !matchesKeyword(doc, keyword))
            {
                continue;
            }
            RagSearchResult result = new RagSearchResult();
            result.setDocId(doc.getDocId());
            result.setTitle(doc.getDocName());
            result.setContent(buildMockContent(query, doc));
            result.setScopeCode(doc.getScopeCode());
            result.setLevel(doc.getSecurityLevel());
            result.setOwnerGroupCode(doc.getOwnerGroupCode());
            result.setOwnerGroupName(doc.getOwnerGroupName());
            result.setMetadataStatus(doc.getMetadataStatus());
            result.setPassed(false);
            result.setFilterReason("");
            results.add(result);
        }

        return results;
    }

    /** 演示模式按文档 ID 或名称匹配；不承担真实向量召回。 */
    private boolean matchesKeyword(SysRagDoc doc, String keyword)
    {
        return keyword.isEmpty()
                || containsKeyword(doc.getDocId(), keyword)
                || containsKeyword(doc.getDocName(), keyword);
    }

    private boolean containsKeyword(String value, String keyword)
    {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    /**
     * 构造模拟内容。
     */
    private String buildMockContent(String query, SysRagDoc doc)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("模拟检索命中文档：");
        builder.append(doc.getDocName());
        builder.append("；用户查询：");
        builder.append(query == null ? "" : query);
        builder.append("；文档知悉范围：");
        builder.append(doc.getScopeCode());
        builder.append("；文档密级：");
        builder.append(doc.getSecurityLevel());
        return builder.toString();
    }
}
