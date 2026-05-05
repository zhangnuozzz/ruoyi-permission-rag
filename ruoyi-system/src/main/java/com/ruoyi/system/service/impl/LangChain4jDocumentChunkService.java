package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import com.ruoyi.system.config.RagFileProperties;
import com.ruoyi.system.domain.RagFileChunk;

/**
 * LangChain4j文件切块服务
 *
 * @author fufu
 * @date 2026-05-05
 */
@Service
public class LangChain4jDocumentChunkService
{
    private final RagFileProperties properties;

    public LangChain4jDocumentChunkService(RagFileProperties properties)
    {
        this.properties = properties;
    }

    public List<RagFileChunk> split(String fileId, String content, String securityLevel, String groupId, String groupName)
    {
        DocumentSplitter splitter = DocumentSplitters.recursive(properties.getChunkSize(), properties.getChunkOverlap());
        List<TextSegment> segments = splitter.split(Document.from(content));
        List<RagFileChunk> chunks = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++)
        {
            RagFileChunk chunk = new RagFileChunk();
            chunk.setChunkId(fileId + "-" + (i + 1));
            chunk.setContent(segments.get(i).text());
            chunk.setFileId(fileId);
            chunk.setSecurityLevel(securityLevel);
            chunk.setGroupId(groupId);
            chunk.setGroupName(groupName);
            chunks.add(chunk);
        }
        return chunks;
    }
}
