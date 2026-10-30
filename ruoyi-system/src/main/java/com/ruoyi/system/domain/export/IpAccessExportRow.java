package com.ruoyi.system.domain.export;

import java.util.Date;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.system.domain.SysIpBlacklist;
import com.ruoyi.system.domain.SysIpWhitelist;

/** 黑白名单按相同字段导出，保留规则来源及维护时间。 */
public final class IpAccessExportRow
{
    @Excel(name = "规则编号")
    private final Long ruleId;
    @Excel(name = "名单类型")
    private final String listType;
    @Excel(name = "IP地址", width = 24)
    private final String ipaddr;
    @Excel(name = "配置原因", width = 36)
    private final String reason;
    @Excel(name = "状态", readConverterExp = "0=启用,1=停用")
    private final String status;
    @Excel(name = "创建时间", dateFormat = "yyyy-MM-dd HH:mm:ss", width = 24)
    private final Date createTime;
    @Excel(name = "更新时间", dateFormat = "yyyy-MM-dd HH:mm:ss", width = 24)
    private final Date updateTime;
    @Excel(name = "备注", width = 36)
    private final String remark;

    public IpAccessExportRow(SysIpBlacklist rule)
    {
        this(rule.getBlacklistId(), "黑名单", rule.getIpaddr(), rule.getReason(), rule.getStatus(),
                rule.getCreateTime(), rule.getUpdateTime(), rule.getRemark());
    }

    public IpAccessExportRow(SysIpWhitelist rule)
    {
        this(rule.getWhitelistId(), "白名单", rule.getIpaddr(), rule.getReason(), rule.getStatus(),
                rule.getCreateTime(), rule.getUpdateTime(), rule.getRemark());
    }

    private IpAccessExportRow(Long id, String type, String ip, String cause, String state,
            Date created, Date updated, String note)
    {
        ruleId = id;
        listType = type;
        ipaddr = ip;
        reason = cause;
        status = state;
        createTime = created == null ? null : new Date(created.getTime());
        updateTime = updated == null ? null : new Date(updated.getTime());
        remark = note;
    }
}
