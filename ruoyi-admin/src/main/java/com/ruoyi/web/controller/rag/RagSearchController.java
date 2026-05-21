package com.ruoyi.web.controller.rag;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.permission.PolicyDecisionResult;
import com.ruoyi.system.domain.rag.RagAuditLog;
import com.ruoyi.system.domain.rag.RagSearchRequest;
import com.ruoyi.system.domain.rag.RagSearchResult;
import com.ruoyi.system.service.IPermissionContextService;
import com.ruoyi.system.service.IPolicyDecisionService;
import com.ruoyi.system.service.IRagAuditLogService;
import com.ruoyi.system.service.IRagDocMockSearchService;
import com.ruoyi.system.service.IRagSecondFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 安全检索入口控制器
 *
 * 当前版本完成：
 * 当前登录用户 -> 权限上下文 -> 策略决策 -> metadata filter 生成
 * -> 从 sys_rag_doc 读取模拟候选结果 -> 二次过滤 -> 审计日志记录
 */
@RestController
@RequestMapping("/rag")
public class RagSearchController
{
    @Autowired
    private IPermissionContextService permissionContextService;

    @Autowired
    private IPolicyDecisionService policyDecisionService;

    @Autowired
    private IRagAuditLogService ragAuditLogService;

    @Autowired
    private IRagSecondFilterService ragSecondFilterService;

    @Autowired
    private IRagDocMockSearchService ragDocMockSearchService;

    /**
     * RAG 安全检索入口。
     *
     * 当前版本暂不真正调用向量数据库和大模型。
     * 先从 sys_rag_doc 表读取文档权限标签，模拟候选检索结果。
     *
     * @param request 检索请求
     * @return 预处理、策略决策与二次过滤结果
     */
    @PostMapping("/search")
    public AjaxResult search(@RequestBody RagSearchRequest request)
    {
        long startTime = System.currentTimeMillis();

        Long userId = SecurityUtils.getLoginUser().getUser().getUserId();
        String userName = SecurityUtils.getUsername();
        Boolean admin = SecurityUtils.isAdmin(userId);

        PermissionContext context = permissionContextService.buildContext(userId, userName, admin);
        PolicyDecisionResult decision = policyDecisionService.decide(context);

        List<RagSearchResult> rawResults = ragDocMockSearchService.search(request.getQuery());
        List<RagSearchResult> filteredResults = ragSecondFilterService.filter(context, rawResults);
        List<RagSearchResult> rejectedResults = buildRejectedResults(rawResults, filteredResults);

        long costTime = System.currentTimeMillis() - startTime;

        recordAuditLog(request, context, decision, costTime);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("query", request.getQuery());
        result.put("userId", context.getUserId());
        result.put("userName", context.getUserName());
        result.put("admin", context.getAdmin());
        result.put("groupCodes", context.getGroupCodes());
        result.put("scopeCodes", context.getScopeCodes());
        result.put("policyCount", context.getPolicies() == null ? 0 : context.getPolicies().size());

        result.put("allowAccess", decision.getAllowAccess());
        result.put("denyReasons", decision.getDenyReasons());
        result.put("metadataFilter", decision.getMetadataFilter());
        result.put("decisionMessage", decision.getMessage());

        result.put("rawResultCount", rawResults == null ? 0 : rawResults.size());
        result.put("filteredResultCount", filteredResults == null ? 0 : filteredResults.size());
        result.put("rejectedResultCount", rejectedResults == null ? 0 : rejectedResults.size());
        result.put("rawResults", rawResults);
        result.put("filteredResults", filteredResults);
        result.put("rejectedResults", rejectedResults);
        result.put("costTime", costTime);

        if (Boolean.FALSE.equals(decision.getAllowAccess()))
        {
            AjaxResult ajax = AjaxResult.error("请求被权限策略拦截");
            ajax.put("data", result);
            return ajax;
        }

        result.put("message", "请求已通过策略决策，并基于 sys_rag_doc 完成模拟检索结果二次过滤与审计留痕");
        return (AjaxResult) AjaxResult.success(result);
    }

    /**
     * 根据原始候选结果和过滤后结果，计算被二次过滤拦截的文档。
     *
     * 该结果主要用于前端演示和审计解释：
     * 1. rawResults 表示模拟向量检索命中的候选文档；
     * 2. filteredResults 表示权限过滤后允许返回给用户的文档；
     * 3. rejectedResults 表示命中但因权限不匹配被过滤的文档。
     */
    private List<RagSearchResult> buildRejectedResults(List<RagSearchResult> rawResults, List<RagSearchResult> filteredResults)
    {
        List<RagSearchResult> rejectedResults = new ArrayList<RagSearchResult>();

        if (rawResults == null || rawResults.isEmpty())
        {
            return rejectedResults;
        }

        for (RagSearchResult raw : rawResults)
        {
            boolean passed = false;

            if (filteredResults != null)
            {
                for (RagSearchResult filtered : filteredResults)
                {
                    if (raw.getDocId() != null && raw.getDocId().equals(filtered.getDocId()))
                    {
                        passed = true;
                        break;
                    }
                }
            }

            if (!passed)
            {
                raw.setPassed(false);
                if (raw.getFilterReason() == null || raw.getFilterReason().length() == 0)
                {
                    raw.setFilterReason("文档 scopeCode 不在当前用户可访问范围内，已被二次权限过滤拦截");
                }
                rejectedResults.add(raw);
            }
        }

        return rejectedResults;
    }

    /**
     * 记录 RAG 检索审计日志。
     */
    private void recordAuditLog(RagSearchRequest request, PermissionContext context,
                                PolicyDecisionResult decision, long costTime)
    {
        RagAuditLog auditLog = new RagAuditLog();

        auditLog.setUserId(context.getUserId());
        auditLog.setUserName(context.getUserName());
        auditLog.setQueryText(request == null ? null : request.getQuery());
        auditLog.setGroupCodes(joinList(context.getGroupCodes()));
        auditLog.setScopeCodes(joinList(context.getScopeCodes()));
        auditLog.setMetadataFilter(decision.getMetadataFilter());
        auditLog.setAllowAccess(Boolean.TRUE.equals(decision.getAllowAccess()) ? "0" : "1");
        auditLog.setDenyReasons(joinList(decision.getDenyReasons()));
        auditLog.setCostTime(costTime);

        ragAuditLogService.record(auditLog);
    }

    /**
     * 将字符串列表转换为逗号分隔字符串。
     */
    private String joinList(List<String> list)
    {
        if (list == null || list.isEmpty())
        {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++)
        {
            if (i > 0)
            {
                builder.append(",");
            }
            builder.append(list.get(i));
        }
        return builder.toString();
    }
}
