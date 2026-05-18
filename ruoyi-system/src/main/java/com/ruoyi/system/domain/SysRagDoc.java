package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 文档权限标签对象 sys_rag_doc
 * 
 * @author zhangnuo
 * @date 2026-05-06
 */
public class SysRagDoc extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 文档ID */
    @Excel(name = "文档ID")
    private String docId;

    /** 文档名称 */
    @Excel(name = "文档名称")
    private String docName;

    /** 知悉范围编码 */
    @Excel(name = "知悉范围编码")
    private String scopeCode;

    /** 文档密级 */
    @Excel(name = "文档密级")
    private String securityLevel;

    /** 所属用户组编码 */
    @Excel(name = "所属用户组编码")
    private String ownerGroupCode;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setDocId(String docId) 
    {
        this.docId = docId;
    }

    public String getDocId() 
    {
        return docId;
    }

    public void setDocName(String docName) 
    {
        this.docName = docName;
    }

    public String getDocName() 
    {
        return docName;
    }

    public void setScopeCode(String scopeCode) 
    {
        this.scopeCode = scopeCode;
    }

    public String getScopeCode() 
    {
        return scopeCode;
    }

    public void setSecurityLevel(String securityLevel) 
    {
        this.securityLevel = securityLevel;
    }

    public String getSecurityLevel() 
    {
        return securityLevel;
    }

    public void setOwnerGroupCode(String ownerGroupCode) 
    {
        this.ownerGroupCode = ownerGroupCode;
    }

    public String getOwnerGroupCode() 
    {
        return ownerGroupCode;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag() 
    {
        return delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("docId", getDocId())
            .append("docName", getDocName())
            .append("scopeCode", getScopeCode())
            .append("securityLevel", getSecurityLevel())
            .append("ownerGroupCode", getOwnerGroupCode())
            .append("status", getStatus())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .toString();
    }
}
