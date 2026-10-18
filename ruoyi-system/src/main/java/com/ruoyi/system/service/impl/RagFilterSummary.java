package com.ruoyi.system.service.impl;

import java.util.LinkedHashSet;
import java.util.Set;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.system.domain.SysRagAuditLog;

/** 区分业务停用与权限异常；证据不完整时保留风险告警。 */
final class RagFilterSummary
{
    private final int riskBlockedCount;
    private final int disabledCount;
    private final boolean verified;

    private RagFilterSummary(int riskBlockedCount, int disabledCount, boolean verified)
    {
        this.riskBlockedCount = riskBlockedCount;
        this.disabledCount = disabledCount;
        this.verified = verified;
    }

    static RagFilterSummary from(SysRagAuditLog log)
    {
        int total = log.getBlockedCount() == null ? 0 : Math.max(0, log.getBlockedCount());
        String detail = log.getBlockedResultsJson();
        String summary = log.getBlockedReasons();
        if ((detail == null || detail.trim().isEmpty()) && total == 0
                && (summary == null || summary.trim().isEmpty()))
        {
            return new RagFilterSummary(0, 0, true);
        }
        try
        {
            JSONArray rows = JSON.parseArray(detail);
            if (rows == null || rows.size() != total)
            {
                return unverified(Math.max(total, rows == null ? 0 : rows.size()));
            }
            Set<String> reasons = new LinkedHashSet<String>();
            int disabled = 0;
            for (Object value : rows)
            {
                if (!(value instanceof JSONObject))
                {
                    return unverified(total);
                }
                JSONObject row = (JSONObject) value;
                Object reason = row.get("blockedReason");
                if (!(reason instanceof String) || ((String) reason).trim().isEmpty())
                {
                    return unverified(total);
                }
                reasons.add((String) reason);
                Object docId = row.get("docId");
                if ("DOCUMENT_STATUS_NOT_ACTIVE".equals(reason)
                        && "DISABLED".equals(row.get("metadataStatus"))
                        && Boolean.FALSE.equals(row.get("passed"))
                        && docId instanceof String && !((String) docId).trim().isEmpty())
                {
                    disabled++;
                }
            }
            Set<String> declared = new LinkedHashSet<String>();
            if (summary != null && !summary.trim().isEmpty())
            {
                for (String reason : summary.split(","))
                {
                    declared.add(reason.trim());
                }
            }
            if (!reasons.equals(declared))
            {
                return unverified(total);
            }
            return new RagFilterSummary(total - disabled, disabled, true);
        }
        catch (RuntimeException exception)
        {
            return unverified(total);
        }
    }

    private static RagFilterSummary unverified(int total)
    {
        return new RagFilterSummary(Math.max(1, total), 0, false);
    }

    int getRiskBlockedCount() { return riskBlockedCount; }
    int getDisabledCount() { return disabledCount; }
    boolean isVerified() { return verified; }
}
