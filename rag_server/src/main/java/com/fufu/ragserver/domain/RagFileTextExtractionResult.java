package com.fufu.ragserver.domain;

/**
 * 文件文本抽取结果
 *
 * @author fufu
 * @date 2026-07-10
 */
public class RagFileTextExtractionResult
{
    private String text;
    private String fileType;
    private String parseMethod;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getParseMethod() { return parseMethod; }
    public void setParseMethod(String parseMethod) { this.parseMethod = parseMethod; }
}
