package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.SecurityUtils;

import com.ruoyi.system.domain.SysRagAuditLog;
import com.ruoyi.system.domain.SysRagBehaviorAlert;
import com.ruoyi.system.mapper.SysRagBehaviorAlertMapper;
import com.ruoyi.system.service.ISysRagAuditLogService;
import com.ruoyi.system.service.ISysRagBehaviorAlertService;
import com.ruoyi.system.service.SyslogForwardService;

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
    private static final Logger log = LoggerFactory.getLogger(SysRagBehaviorAlertServiceImpl.class);
    @Autowired
    private SysRagBehaviorAlertMapper sysRagBehaviorAlertMapper;

    @Autowired
    private ISysRagAuditLogService sysRagAuditLogService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SyslogForwardService syslogForwardService;

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
    @Transactional(rollbackFor = Exception.class)
    public int handleAlert(Long id, String handledBy, String handleRemark)
    {
        return handleAlert(id, "CONFIRM", handledBy, handleRemark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int handleAlert(Long id, String action, String handledBy, String handleRemark)
    {
        String realAction = action == null || action.trim().isEmpty()
                ? "CONFIRM" : action.trim().toUpperCase(Locale.ROOT);
        if (!"CONFIRM".equals(realAction) && !"IGNORE".equals(realAction)
                && !"BLOCK_USER".equals(realAction) && !"LIMIT_USER".equals(realAction))
        {
            throw new CustomException("处置动作无效，请重新选择");
        }
        if (id == null || id <= 0 || handledBy == null || handledBy.trim().isEmpty()
                || handledBy.length() > 64)
        {
            throw new CustomException("告警或处理人信息无效，请刷新后重试");
        }
        String realRemark = handleRemark == null ? "" : handleRemark.trim();
        if (realRemark.isEmpty() || realRemark.length() > 800)
        {
            throw new CustomException("请输入 1 至 800 字的处理说明");
        }

        // 锁定告警后核对状态，防止两个管理员覆盖彼此的处置结果。
        SysRagBehaviorAlert oldAlert = sysRagBehaviorAlertMapper.selectSysRagBehaviorAlertByIdForUpdate(id);
        if (oldAlert == null)
        {
            throw new CustomException("告警不存在，请刷新后重试");
        }
        if (!"unhandled".equals(oldAlert.getStatus()))
        {
            throw new CustomException("告警已被处理，请刷新查看最新处置信息");
        }

        if ("BLOCK_USER".equals(realAction))
        {
            lockUserAttributes(oldAlert.getUserId());
            blockUserTemporarily(oldAlert.getUserId(), "行为告警临时封禁，alertId=" + id);
            realRemark = appendRemark(realRemark, "处置动作：临时封禁该用户 30 分钟。");
        }
        else if ("LIMIT_USER".equals(realAction))
        {
            Map<String, Object> attributes = lockUserAttributes(oldAlert.getUserId());
            if (!"HIGH".equals(attributes.get("riskLevel")))
            {
                limitUserRisk(oldAlert.getUserId());
            }
            realRemark = appendRemark(realRemark, "处置动作：将该用户标记为高风险，后续请求按高风险规则校验。");
        }
        else
        {
            realRemark = appendRemark(realRemark, "IGNORE".equals(realAction)
                    ? "处置动作：忽略告警。" : "处置动作：确认告警。");
        }

        SysRagBehaviorAlert alert = new SysRagBehaviorAlert();
        alert.setId(id);
        alert.setStatus(resolveStatus(realAction));
        alert.setHandledBy(handledBy.trim());
        alert.setHandledTime(new Date());
        alert.setHandleRemark(realRemark);
        if (sysRagBehaviorAlertMapper.updateUnhandledAlert(alert) != 1)
        {
            throw new CustomException("告警处置未保存，请刷新后核对；本次用户状态修改已回滚");
        }
        return 1;
    }

    @Override
    public int analyzeRagAuditLogById(Long auditLogId)
    {
        if (auditLogId == null)
        {
            return 0;
        }

        SysRagAuditLog log = sysRagAuditLogService.selectSysRagAuditLogById(auditLogId);
        return analyzeOneLog(log);
    }

    private int analyzeOneLog(SysRagAuditLog log)
    {
        if (log == null || log.getId() == null)
        {
            return 0;
        }

        int count = 0;

        Integer riskScore = safeInt(log.getRiskScore());
        RagFilterSummary filterSummary = RagFilterSummary.from(log);
        int blockedCount = filterSummary.getRiskBlockedCount();
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
                    "查询风险分数达到严重级别，riskScore=" + riskScore
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
                    "本次检索存在大量权限或元数据异常拦截，riskBlockedCount=" + blockedCount
                            + "，blockedReasons=" + safeText(blockedReasons)
            );
        }
        else if (blockedCount > 0)
        {
            count += insertAlertIgnoreDuplicate(
                    log,
                    "SECOND_FILTER_BLOCK",
                    "medium",
                    "平台复核拦截了权限或元数据异常片段，riskBlockedCount=" + blockedCount
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
                    "查询安全上下文识别到重复查询模式"
            );
        }

        if (containsRiskReason(secureContextJson, "RISK_REQUEST_RATE_TOO_HIGH")
                || containsRiskReason(secureContextJson, "BBAC_REQUEST_RATE_LIMITED"))
        {
            count += insertAlertIgnoreDuplicate(
                    log,
                    "HIGH_FREQUENCY_ACCESS",
                    "medium",
                    "查询安全上下文识别到单位时间访问频次过高"
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

    private int insertAlertIgnoreDuplicate(SysRagAuditLog source, String type, String level, String reason)
    {
        SysRagBehaviorAlert alert = new SysRagBehaviorAlert();
        alert.setSourceLogId(source.getId());
        alert.setUserId(source.getUserId());
        alert.setUserName(source.getUserName());
        alert.setAlertType(type);
        alert.setAlertLevel(level);
        alert.setAlertReason(fitText(reason, 500));
        alert.setQueryText(source.getQueryText());
        alert.setAllowAccess(source.getAllowAccess());
        alert.setCostTime(source.getCostTime());
        alert.setStatus("unhandled");
        alert.setRemark(fitText(buildRemark(source), 500));
        int rows;
        try
        {
            rows = sysRagBehaviorAlertMapper.insertSysRagBehaviorAlert(alert);
        }
        catch (DuplicateKeyException exception)
        {
            return 0;
        }
        if (rows > 0)
        {
            try
            {
                syslogForwardService.forwardAlert(alert);
            }
            catch (RuntimeException exception)
            {
                log.warn("告警已保存，Syslog 转发异常，告警ID：{}", alert.getId(), exception);
            }
        }
        return rows;
    }

    /** 仅对存在且启用的普通账号执行风险处置，不覆盖停用或已有封禁状态。 */
    private Map<String, Object> lockUserAttributes(Long userId)
    {
        if (userId == null || userId <= 0 || SecurityUtils.isAdmin(userId))
        {
            throw new CustomException("该告警没有可处置的普通账号，请选择确认或忽略");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select a.access_status accessStatus, a.risk_level riskLevel " +
                "from sys_user_security_attr a join sys_user u on u.user_id = a.user_id " +
                "where a.user_id = ? and u.status = '0' and u.del_flag = '0' for update", userId);
        if (rows.size() != 1 || !"ACTIVE".equals(rows.get(0).get("accessStatus")))
        {
            throw new CustomException("人员不存在、已停用或已封禁，请刷新核对；未修改人员状态");
        }
        return rows.get(0);
    }

    private void blockUserTemporarily(Long userId, String reason)
    {
        int rows = jdbcTemplate.update(
                "update sys_user_security_attr " +
                "set access_status='LOCKED', lock_until=date_add(now(), interval 30 minute), lock_reason=? " +
                "where user_id=? and access_status='ACTIVE'", reason, userId);
        if (rows != 1)
        {
            throw new CustomException("人员封禁未保存，本次告警处置已撤销");
        }
    }

    private void limitUserRisk(Long userId)
    {
        int rows = jdbcTemplate.update(
                "update sys_user_security_attr set risk_level='HIGH' where user_id=? and access_status='ACTIVE'", userId);
        if (rows != 1)
        {
            throw new CustomException("人员风险标记未保存，本次告警处置已撤销");
        }
    }

    private String fitText(String value, int limit)
    {
        if (value == null || value.length() <= limit)
        {
            return value;
        }
        return value.substring(0, limit - 3) + "...";
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
        RagFilterSummary summary = RagFilterSummary.from(log);
        builder.append("; riskBlockedCount=").append(summary.getRiskBlockedCount());
        builder.append("; disabledCount=").append(summary.getDisabledCount());
        builder.append("; filterDetailsVerified=").append(summary.isVerified());
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
        if (text == null || key == null)
        {
            return false;
        }
        try
        {
            JSONObject context = JSON.parseObject(text);
            Object reasons = context == null ? null : context.get("reasons");
            return reasons instanceof JSONArray && ((JSONArray) reasons).contains(key);
        }
        catch (RuntimeException exception)
        {
            // 历史非结构化记录保守兼容；新记录只读取实际决策原因。
            return text.contains(key);
        }
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
