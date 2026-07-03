package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.system.domain.SysRagAuditLog;
import com.ruoyi.system.domain.SysRagBehaviorAlert;
import com.ruoyi.system.mapper.SysRagBehaviorAlertMapper;
import com.ruoyi.system.service.ISysRagAuditLogService;
import com.ruoyi.system.service.ISysRagBehaviorAlertService;

/**
 * RAG行为分析告警Service业务层处理
 *
 * VACP 行为分析规则：
 * 1. 权限拒绝：allow_access = 0；
 * 2. 高风险查询：risk_score >= 70；
 * 3. 严重风险拦截：risk_score >= 90；
 * 4. 大量结果被拦截：blocked_count >= 5；
 * 5. 敏感词查询：secure_context_json 中包含敏感词风险标记；
 * 6. 慢查询：cost_time >= 5000ms。
 */
@Service
public class SysRagBehaviorAlertServiceImpl implements ISysRagBehaviorAlertService
{
    @Autowired
    private SysRagBehaviorAlertMapper sysRagBehaviorAlertMapper;

    @Autowired
    private ISysRagAuditLogService sysRagAuditLogService;

    @Override
    public SysRagBehaviorAlert selectSysRagBehaviorAlertById(Long id)
    {
        return sysRagBehaviorAlertMapper.selectSysRagBehaviorAlertById(id);
    }

    @Override
    public List<SysRagBehaviorAlert> selectSysRagBehaviorAlertList(SysRagBehaviorAlert sysRagBehaviorAlert)
    {
        return sysRagBehaviorAlertMapper.selectSysRagBehaviorAlertList(sysRagBehaviorAlert);
    }

    @Override
    public int insertSysRagBehaviorAlert(SysRagBehaviorAlert sysRagBehaviorAlert)
    {
        return sysRagBehaviorAlertMapper.insertSysRagBehaviorAlert(sysRagBehaviorAlert);
    }

    @Override
    public int updateSysRagBehaviorAlert(SysRagBehaviorAlert sysRagBehaviorAlert)
    {
        return sysRagBehaviorAlertMapper.updateSysRagBehaviorAlert(sysRagBehaviorAlert);
    }

    @Override
    public int deleteSysRagBehaviorAlertByIds(Long[] ids)
    {
        return sysRagBehaviorAlertMapper.deleteSysRagBehaviorAlertByIds(ids);
    }

    @Override
    public int deleteSysRagBehaviorAlertById(Long id)
    {
        return sysRagBehaviorAlertMapper.deleteSysRagBehaviorAlertById(id);
    }


    @Override
    public int handleAlert(Long id, String handledBy, String handleRemark)
    {
        SysRagBehaviorAlert alert = new SysRagBehaviorAlert();
        alert.setId(id);
        alert.setStatus("handled");
        alert.setHandledBy(handledBy == null || handledBy.length() == 0 ? "admin" : handledBy);
        alert.setHandledTime(new Date());
        alert.setHandleRemark(handleRemark == null ? "" : handleRemark);
        return sysRagBehaviorAlertMapper.updateSysRagBehaviorAlert(alert);
    }

    @Override
    public int analyzeRagAuditLogs()
    {
        int count = 0;
        List<SysRagAuditLog> logs = sysRagAuditLogService.selectSysRagAuditLogList(new SysRagAuditLog());

        for (SysRagAuditLog log : logs)
        {
            if (log == null || log.getId() == null)
            {
                continue;
            }

            Integer riskScore = safeInt(log.getRiskScore());
            Integer blockedCount = safeInt(log.getBlockedCount());
            String allowAccess = log.getAllowAccess();
            String blockedReasons = log.getBlockedReasons();
            String secureContextJson = log.getSecureContextJson();

            if ("0".equals(allowAccess))
            {
                count += insertAlertIgnoreDuplicate(
                        log,
                        "DENY_ACCESS",
                        "high",
                        "访问请求被查询安全上下文或权限策略拒绝；denyReasons=" + safeText(log.getDenyReasons())
                );
            }

            if (riskScore >= 90)
            {
                count += insertAlertIgnoreDuplicate(
                        log,
                        "CRITICAL_RISK_QUERY",
                        "critical",
                        "查询风险分数达到严重级别，riskScore=" + riskScore + "，系统已执行高风险拦截或强限制"
                );
            }
            else if (riskScore >= 70)
            {
                count += insertAlertIgnoreDuplicate(
                        log,
                        "HIGH_RISK_QUERY",
                        "high",
                        "查询风险分数达到高风险级别，riskScore=" + riskScore
                );
            }

            if (blockedCount >= 5)
            {
                count += insertAlertIgnoreDuplicate(
                        log,
                        "MASSIVE_RESULT_BLOCK",
                        "medium",
                        "本次检索存在大量结果被二次过滤拦截，blockedCount=" + blockedCount
                                + "，blockedReasons=" + safeText(blockedReasons)
                );
            }
            else if (blockedCount > 0)
            {
                count += insertAlertIgnoreDuplicate(
                        log,
                        "SECOND_FILTER_BLOCK",
                        "medium",
                        "安全向量检索二次过滤拦截了不符合权限的数据，blockedCount=" + blockedCount
                                + "，blockedReasons=" + safeText(blockedReasons)
                );
            }

            if (containsRiskReason(secureContextJson, "RISK_QUERY_CONTAINS_SENSITIVE_KEYWORD"))
            {
                count += insertAlertIgnoreDuplicate(
                        log,
                        "SENSITIVE_QUERY",
                        "high",
                        "查询安全上下文识别到敏感词访问行为"
                );
            }

            if (containsRiskReason(secureContextJson, "RISK_TOPK_TOO_LARGE"))
            {
                count += insertAlertIgnoreDuplicate(
                        log,
                        "LARGE_TOPK_QUERY",
                        "low",
                        "查询请求 topK 过大，系统已自动压缩安全召回数量"
                );
            }

            Long costTime = log.getCostTime();
            if (costTime != null && costTime.longValue() >= 5000L)
            {
                count += insertAlertIgnoreDuplicate(
                        log,
                        "SLOW_QUERY",
                        "medium",
                        "RAG检索耗时超过5000毫秒，可能存在异常慢查询，costTime=" + costTime
                );
            }
        }

        return count;
    }

    private int insertAlertIgnoreDuplicate(SysRagAuditLog log, String type, String level, String reason)
    {
        try
        {
            SysRagBehaviorAlert alert = new SysRagBehaviorAlert();
            alert.setSourceLogId(log.getId());
            alert.setUserId(log.getUserId());
            alert.setUserName(log.getUserName());
            alert.setAlertType(type);
            alert.setAlertLevel(level);
            alert.setAlertReason(reason);
            alert.setQueryText(log.getQueryText());
            alert.setAllowAccess(log.getAllowAccess());
            alert.setCostTime(log.getCostTime());
            alert.setStatus("unhandled");
            alert.setRemark(buildRemark(log));
            return sysRagBehaviorAlertMapper.insertSysRagBehaviorAlert(alert);
        }
        catch (Exception e)
        {
            // 主要用于忽略唯一索引重复插入
            return 0;
        }
    }

    private String buildRemark(SysRagAuditLog log)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("riskScore=").append(safeInt(log.getRiskScore()));
        builder.append("; limitedQuery=").append(safeText(log.getLimitedQuery()));
        builder.append("; passedCount=").append(safeInt(log.getPassedCount()));
        builder.append("; blockedCount=").append(safeInt(log.getBlockedCount()));
        builder.append("; blockedReasons=").append(safeText(log.getBlockedReasons()));
        return builder.toString();
    }

    private boolean containsRiskReason(String text, String key)
    {
        return text != null && key != null && text.contains(key);
    }

    private Integer safeInt(Integer value)
    {
        return value == null ? 0 : value;
    }

    private String safeText(String value)
    {
        return value == null ? "" : value;
    }
}
