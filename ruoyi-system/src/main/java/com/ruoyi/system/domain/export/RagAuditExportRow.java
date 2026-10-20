package com.ruoyi.system.domain.export;

import java.util.Date;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.system.domain.SysRagAuditLog;
import com.ruoyi.system.domain.rag.QueryExecutionStatus;

/** 检索审计导出行，显式限定导出字段，保留记录编号和发生时间。 */
public final class RagAuditExportRow
{
    @Excel(name = "审计ID", sort = 0)
    private final Long auditId;

    @Excel(name = "审计时间", sort = 1, width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private final Date createTime;

    @Excel(name = "用户ID", sort = 2)
    private final Long userId;

    @Excel(name = "用户名", sort = 3)
    private final String userName;

    @Excel(name = "原始查询内容", sort = 4)
    private final String queryText;

    @Excel(name = "用户组编码集合", sort = 5)
    private final String groupCodes;

    @Excel(name = "知悉范围编码集合", sort = 6)
    private final String scopeCodes;

    @Excel(name = "元数据过滤条件", sort = 7)
    private final String metadataFilter;

    @Excel(name = "请求准入", sort = 8, readConverterExp = "1=获准执行,0=请求拒绝")
    private final String allowAccess;

    @Excel(name = "请求拒绝原因", sort = 9)
    private final String denyReasons;

    @Excel(name = "风险分数", sort = 10)
    private final Integer riskScore;

    @Excel(name = "是否受限查询", sort = 11, readConverterExp = "1=是,0=否")
    private final String limitedQuery;

    @Excel(name = "可访问候选数量", sort = 12)
    private final Integer passedCount;

    @Excel(name = "已拦截候选数量", sort = 13)
    private final Integer blockedCount;

    @Excel(name = "文档拦截原因汇总", sort = 14)
    private final String blockedReasons;

    @Excel(name = "耗时，单位毫秒", sort = 15)
    private final Long costTime;

    @Excel(name = "执行结果", sort = 16)
    private final String executionStatus;

    public RagAuditExportRow(SysRagAuditLog source)
    {
        this.auditId = source.getId();
        this.createTime = source.getCreateTime();
        this.userId = source.getUserId();
        this.userName = source.getUserName();
        this.queryText = source.getQueryText();
        this.groupCodes = source.getGroupCodes();
        this.scopeCodes = source.getScopeCodes();
        this.metadataFilter = source.getMetadataFilter();
        this.allowAccess = source.getAllowAccess();
        this.denyReasons = source.getDenyReasons();
        this.riskScore = source.getRiskScore();
        this.limitedQuery = source.getLimitedQuery();
        this.passedCount = source.getPassedCount();
        this.blockedCount = source.getBlockedCount();
        this.blockedReasons = source.getBlockedReasons();
        this.costTime = source.getCostTime();
        this.executionStatus = QueryExecutionStatus.from(source.getAllowAccess(), source.getResponseJson()).getLabel();
    }
}
