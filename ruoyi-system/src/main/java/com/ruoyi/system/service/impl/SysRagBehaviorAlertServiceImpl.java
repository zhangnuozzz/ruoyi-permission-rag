package com.ruoyi.system.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * 1. 权限拒绝；
 * 2. 高风险查询；
 * 3. 严重风险查询；
 * 4. 大量结果被二次过滤拦截；
 * 5. 敏感词查询；
 * 6. topK 过大；
 * 7. 访问频次过高；
 * 8. 重复查询；
 * 9. 慢查询。
 */
@Service
public class SysRagBehaviorAlertServiceImpl implements ISysRagBehaviorAlertService
{
    @Autowired
    private SysRagBehaviorAlertMapper sysRagBehaviorAlertMapper;

    @Autowired
    private ISysRagAuditLogService sysRagAuditLogService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        return handleAlert(id, "CONFIRM", handledBy, handleRemark);
    }

    @Override
    public int handleAlert(Long id, String action, String handledBy, String handleRemark)
    {
        SysRagBehaviorAlert oldAlert = sysRagBehaviorAlertMapper.selectSysRagBehaviorAlertById(id);
        if (oldAlert == null)
        {
            return 0;
        }

        String realAction = action == null || action.length() == 0 ? "CONFIRM" : action.trim().toUpperCase();
        String realHandledBy = handledBy == null || handledBy.length() == 0 ? "admin" : handledBy;
        String realRemark = handleRemark == null ? "" : handleRemark;

        if ("BLOCK_USER".equals(realAction))
        {
            blockUserTemporarily(oldAlert.getUserId(), "行为告警触发临时封禁，alertId=" + id);
            realRemark = appendRemark(realRemark, "处置动作：已临时封禁该用户 30 分钟。");
        }
        else if ("LIMIT_USER".equals(realAction))
        {
            limitUserRisk(oldAlert.getUserId());
            realRemark = appendRemark(realRemark, "处置动作：已将该用户风险等级调整为 HIGH，后续查询将更容易被限制。");
        }
        else if ("IGNORE".equals(realAction))
        {
            realRemark = appendRemark(realRemark, "处置动作：已确认该告警为可忽略事件。");
        }
        else
        {
            realRemark = appendRemark(realRemark, "处置动作：已确认告警并完成核查。");
        }

        SysRagBehaviorAlert alert = new SysRagBehaviorAlert();
        alert.setId(id);
        alert.setStatus(resolveStatus(realAction));
        alert.setHandledBy(realHandledBy);
        alert.setHandledTime(new Date());
        alert.setHandleRemark(realRemark);
        return sysRagBehaviorAlertMapper.updateSysRagBehaviorAlert(alert);
    }


    @Override
    public int analyzeRagAuditLogById(Long auditLogId)
    {
        if (auditLogId == null)
        {
            return 0;
        }

        SysRagAuditLog query = new SysRagAuditLog();
        query.setId(auditLogId);

        List<SysRagAuditLog> logs = sysRagAuditLogService.selectSysRagAuditLogList(query);
        if (logs == null || logs.isEmpty())
        {
            SysRagAuditLog one = sysRagAuditLogService.selectSysRagAuditLogById(auditLogId);
            if (one == null)
            {
                return 0;
            }
            return analyzeOneLog(one);
        }

        int count = 0;
        for (SysRagAuditLog log : logs)
        {
            count += analyzeOneLog(log);
        }
        return count;
    }

    private int analyzeOneLog(SysRagAuditLog log)
    {
        if (log == null || log.getId() == null)
        {
            return 0;
        }

        int count = 0;

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

        if (containsRiskReason(secureContextJson, "RISK_REPEAT_QUERY_PATTERN")
                || containsRiskReason(secureContextJson, "BBAC_REPEAT_QUERY_PATTERN_LIMITED"))
        {
            count += insertAlertIgnoreDuplicate(
                    log,
                    "REPEAT_QUERY_PATTERN",
                    "medium",
                    "检测到重复查询模式，系统已执行限制策略"
            );
        }

        if (containsRiskReason(secureContextJson, "RISK_REQUEST_RATE_TOO_HIGH")
                || containsRiskReason(secureContextJson, "BBAC_REQUEST_RATE_LIMITED"))
        {
            count += insertAlertIgnoreDuplicate(
                    log,
                    "HIGH_FREQUENCY_ACCESS",
                    "medium",
                    "检测到单位时间访问频次过高，系统已执行限流策略"
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

        return count;
    }

    @Override
    public int analyzeRagAuditLogs()
    {
        int count = 0;
        List<SysRagAuditLog> logs = sysRagAuditLogService.selectSysRagAuditLogList(new SysRagAuditLog());

        for (SysRagAuditLog log : logs)
        {
            count += analyzeOneLog(log);
        }

        return count;
    }


    @Override
    public String exportSyslog(SysRagBehaviorAlert query)
    {
        List<SysRagBehaviorAlert> alerts = selectSysRagBehaviorAlertList(query == null ? new SysRagBehaviorAlert() : query);
        StringBuilder builder = new StringBuilder();

        if (alerts == null || alerts.isEmpty())
        {
            return "";
        }

        for (SysRagBehaviorAlert alert : alerts)
        {
            builder.append("<134> VACP-RAG-AUDIT");
            builder.append(" alert_id=").append(alert.getId());
            builder.append(" source_log_id=").append(alert.getSourceLogId());
            builder.append(" user_id=").append(alert.getUserId());
            builder.append(" user_name=\"").append(escapeSyslog(alert.getUserName())).append("\"");
            builder.append(" alert_type=").append(escapeSyslog(alert.getAlertType()));
            builder.append(" alert_level=").append(escapeSyslog(alert.getAlertLevel()));
            builder.append(" status=").append(escapeSyslog(alert.getStatus()));
            builder.append(" allow_access=").append(escapeSyslog(alert.getAllowAccess()));
            builder.append(" cost_time=").append(alert.getCostTime());
            builder.append(" query=\"").append(escapeSyslog(alert.getQueryText())).append("\"");
            builder.append(" reason=\"").append(escapeSyslog(alert.getAlertReason())).append("\"");
            builder.append(" remark=\"").append(escapeSyslog(alert.getRemark())).append("\"");
            builder.append("\n");
        }

        return builder.toString();
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
            return 0;
        }
    }

    private void blockUserTemporarily(Long userId, String reason)
    {
        if (userId == null)
        {
            return;
        }

        jdbcTemplate.update(
                "update sys_user_security_attr " +
                        "set access_status='LOCKED', lock_until=date_add(now(), interval 30 minute), lock_reason=? " +
                        "where user_id=?",
                reason,
                userId
        );
    }

    private void limitUserRisk(Long userId)
    {
        if (userId == null)
        {
            return;
        }

        jdbcTemplate.update(
                "update sys_user_security_attr set risk_level='HIGH' where user_id=?",
                userId
        );
    }

    private String resolveStatus(String action)
    {
        if ("BLOCK_USER".equals(action))
        {
            return "blocked";
        }
        if ("LIMIT_USER".equals(action))
        {
            return "limited";
        }
        if ("IGNORE".equals(action))
        {
            return "ignored";
        }
        return "handled";
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

    private String appendRemark(String oldRemark, String append)
    {
        if (oldRemark == null || oldRemark.length() == 0)
        {
            return append;
        }
        return oldRemark + " " + append;
    }

    private boolean containsRiskReason(String text, String key)
    {
        return text != null && key != null && text.contains(key);
    }

    private Integer safeInt(Integer value)
    {
        return value == null ? 0 : value;
    }

    private String safeText(Object value)
    {
        return value == null ? "" : String.valueOf(value);
    }

    private String escapeSyslog(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}
