package com.ruoyi.web.controller.rag;

import com.alibaba.fastjson.JSON;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.permission.PolicyDecisionResult;
import com.ruoyi.system.domain.rag.RagAuditLog;
import com.ruoyi.system.domain.rag.RagSearchRequest;
import com.ruoyi.system.domain.rag.RagSearchResult;
import com.ruoyi.system.domain.security.SecureQueryContext;
import com.ruoyi.system.service.IPermissionContextService;
import com.ruoyi.system.service.IPolicyDecisionService;
import com.ruoyi.system.service.IRagAuditLogService;
import com.ruoyi.system.service.IRagDocMockSearchService;
import com.ruoyi.system.service.IRagSecondFilterService;
import com.ruoyi.system.service.IRagRemoteSearchService;
import com.ruoyi.system.service.IRagAnswerService;
import com.ruoyi.system.service.ISecureQueryContextService;
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

    @Autowired
    private IRagRemoteSearchService ragRemoteSearchService;

    @Autowired
    private IRagAnswerService ragAnswerService;

    @Autowired
    private ISecureQueryContextService secureQueryContextService;

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

        /*
         * VACP 查询安全增强：
         * 1. 先生成检索前安全上下文；
         * 2. 对 query 做基础净化；
         * 3. 对 topK 做安全收缩；
         * 4. 生成统一 metadataFilter；
         * 5. 如果安全上下文不允许查询，则直接拦截。
         */
        SecureQueryContext secureQueryContext = secureQueryContextService.buildContext(
                userId,
                userName,
                admin,
                request == null ? null : request.getQuery(),
                request == null ? null : request.getTopK()
        );

        if (request == null)
        {
            request = new RagSearchRequest();
        }

        request.setQuery(secureQueryContext.getSanitizedQuery());
        request.setTopK(secureQueryContext.getSafeTopK());

        PermissionContext context = permissionContextService.buildContext(userId, userName, admin);
        PolicyDecisionResult decision = policyDecisionService.decide(context);

        /*
         * 以查询安全上下文生成的 metadataFilter 为主。
         * 后续接 RAG Server 时，下游统一使用这个标准过滤表达式。
         */
        decision.setMetadataFilter(secureQueryContext.getMetadataFilter());

        if (Boolean.FALSE.equals(secureQueryContext.getAllowQuery()))
        {
            decision.setAllowAccess(false);
            decision.setDenyReasons(secureQueryContext.getReasons());
            decision.setMessage("查询安全上下文拒绝本次检索");

            List<RagSearchResult> emptyResults = new ArrayList<RagSearchResult>();
            long costTime = System.currentTimeMillis() - startTime;

            Map<String, Object> blockedResult = new LinkedHashMap<String, Object>();
            blockedResult.put("query", request.getQuery());
            blockedResult.put("searchMode", "blocked_by_secure_query_context");
            blockedResult.put("topK", request.getTopK());
            blockedResult.put("userId", context.getUserId());
            blockedResult.put("userName", context.getUserName());
            blockedResult.put("admin", context.getAdmin());
            blockedResult.put("allowAccess", false);
            blockedResult.put("denyReasons", secureQueryContext.getReasons());
            blockedResult.put("metadataFilter", secureQueryContext.getMetadataFilter());
            blockedResult.put("secureQueryContext", secureQueryContext);
            blockedResult.put("rawResultCount", 0);
            blockedResult.put("filteredResultCount", 0);
            blockedResult.put("rejectedResultCount", 0);
            blockedResult.put("rawResults", emptyResults);
            blockedResult.put("filteredResults", emptyResults);
            blockedResult.put("rejectedResults", emptyResults);
            blockedResult.put("costTime", costTime);
            blockedResult.put("message", "本次查询被查询安全上下文拦截");

            AjaxResult ajax = AjaxResult.error("请求被查询安全上下文拦截");
            ajax.put("data", blockedResult);
            recordAuditLog(request, context, decision, secureQueryContext, costTime, emptyResults, emptyResults, emptyResults, ajax);
            return ajax;
        }

        List<RagSearchResult> rawResults;

        if (Boolean.TRUE.equals(request.getUseRemote()))
        {
            rawResults = ragRemoteSearchService.search(request, context, decision);
        }
        else
        {
            rawResults = ragDocMockSearchService.search(request.getQuery());
        }

        rawResults = limitResults(rawResults, secureQueryContext.getSafeTopK());

        List<RagSearchResult> filteredResults = ragSecondFilterService.filter(context, rawResults);
        List<RagSearchResult> rejectedResults = buildRejectedResults(rawResults, filteredResults);

        String answer = "";
        String answerModel = "";
        long answerCostTime = 0L;

        if (Boolean.TRUE.equals(decision.getAllowAccess()))
        {
            long answerStartTime = System.currentTimeMillis();
            answerModel = ragAnswerService.getModelName();
            answer = ragAnswerService.generateAnswer(request.getQuery(), filteredResults);
            answerCostTime = System.currentTimeMillis() - answerStartTime;
        }

        long costTime = System.currentTimeMillis() - startTime;


        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("query", request.getQuery());
        result.put("searchMode", Boolean.TRUE.equals(request.getUseRemote()) ? "remote_rag_server" : "mock_sys_rag_doc");
        result.put("topK", request.getTopK() == null ? 5 : request.getTopK());
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
        result.put("secureQueryContext", secureQueryContext);
        result.put("limitedQuery", secureQueryContext.getLimitedQuery());
        result.put("riskScore", secureQueryContext.getRiskScore());
        result.put("safeTopK", secureQueryContext.getSafeTopK());
        result.put("sanitizedQuery", secureQueryContext.getSanitizedQuery());

        result.put("rawResultCount", rawResults == null ? 0 : rawResults.size());
        result.put("filteredResultCount", filteredResults == null ? 0 : filteredResults.size());
        result.put("rejectedResultCount", rejectedResults == null ? 0 : rejectedResults.size());
        result.put("rawResults", rawResults);
        result.put("filteredResults", filteredResults);
        result.put("rejectedResults", rejectedResults);
        result.put("costTime", costTime);
        result.put("answer", answer);
        result.put("answerModel", answerModel);
        result.put("answerCostTime", answerCostTime);
        result.put("answerEnabled", ragAnswerService.isEnabled());

        if (Boolean.FALSE.equals(decision.getAllowAccess()))
        {
            AjaxResult ajax = AjaxResult.error("请求被权限策略拦截");
            ajax.put("data", result);
            recordAuditLog(request, context, decision, secureQueryContext, costTime, rawResults, filteredResults, rejectedResults, ajax);
            return ajax;
        }

        result.put("message", Boolean.TRUE.equals(request.getUseRemote())
                ? "请求已通过策略决策，完成 RAG Server 真实检索、二次过滤、审计留痕与外部模型回答生成"
                : "请求已通过策略决策，完成平台 Mock 检索、二次过滤、审计留痕与外部模型回答生成");

        AjaxResult ajax = AjaxResult.success(result);
        recordAuditLog(request, context, decision, secureQueryContext, costTime, rawResults, filteredResults, rejectedResults, ajax);
        return ajax;
    }

    /**
     * 按安全 topK 截断候选结果。
     */
    private List<RagSearchResult> limitResults(List<RagSearchResult> results, Integer safeTopK)
    {
        if (results == null || results.isEmpty())
        {
            return results;
        }

        int limit = safeTopK == null || safeTopK <= 0 ? 5 : safeTopK;
        if (results.size() <= limit)
        {
            return results;
        }

        return new ArrayList<RagSearchResult>(results.subList(0, limit));
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
                                PolicyDecisionResult decision, SecureQueryContext secureQueryContext,
                                long costTime,
                                List<RagSearchResult> rawResults,
                                List<RagSearchResult> filteredResults,
                                List<RagSearchResult> rejectedResults,
                                Object responseObject)
    {
        RagAuditLog auditLog = new RagAuditLog();

        auditLog.setUserId(context.getUserId());
        auditLog.setUserName(context.getUserName());
        auditLog.setQueryText(request == null ? null : request.getQuery());
        auditLog.setGroupCodes(joinList(context.getGroupCodes()));
        auditLog.setScopeCodes(joinList(context.getScopeCodes()));
        auditLog.setUserContextJson(JSON.toJSONString(context));
        auditLog.setMetadataFilter(decision.getMetadataFilter());
        auditLog.setRequestJson(JSON.toJSONString(request));
        auditLog.setRawResultsJson(JSON.toJSONString(rawResults));
        auditLog.setPassedResultsJson(JSON.toJSONString(filteredResults));
        auditLog.setBlockedResultsJson(JSON.toJSONString(rejectedResults));
        auditLog.setResponseJson(JSON.toJSONString(responseObject));
        auditLog.setAllowAccess(Boolean.TRUE.equals(decision.getAllowAccess()) ? "1" : "0");
        auditLog.setDenyReasons(joinList(decision.getDenyReasons()));

        if (secureQueryContext != null)
        {
            auditLog.setRiskScore(secureQueryContext.getRiskScore());
            auditLog.setLimitedQuery(Boolean.TRUE.equals(secureQueryContext.getLimitedQuery()) ? "1" : "0");
            auditLog.setSecureContextJson(JSON.toJSONString(secureQueryContext));
        }
        else
        {
            auditLog.setRiskScore(0);
            auditLog.setLimitedQuery("0");
            auditLog.setSecureContextJson("");
        }

        auditLog.setPassedCount(filteredResults == null ? 0 : filteredResults.size());
        auditLog.setBlockedCount(rejectedResults == null ? 0 : rejectedResults.size());
        auditLog.setBlockedReasons(joinBlockedReasons(rejectedResults));
        auditLog.setCostTime(costTime);

        ragAuditLogService.record(auditLog);
    }

    /**
     * 汇总被拦截结果的 blockedReason。
     */
    private String joinBlockedReasons(List<RagSearchResult> rejectedResults)
    {
        if (rejectedResults == null || rejectedResults.isEmpty())
        {
            return "";
        }

        List<String> reasons = new ArrayList<String>();
        for (RagSearchResult result : rejectedResults)
        {
            if (result == null)
            {
                continue;
            }

            String reason = result.getBlockedReason();
            if (reason != null && reason.length() > 0 && !reasons.contains(reason))
            {
                reasons.add(reason);
            }
        }

        return joinList(reasons);
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
