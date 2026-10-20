package com.ruoyi.web.controller.rag;

import com.alibaba.fastjson.JSON;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.permission.PolicyDecisionResult;
import com.ruoyi.system.domain.rag.RagAuditLog;
import com.ruoyi.system.domain.rag.RagSearchLimits;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 安全检索入口：查询准入、候选检索、文档鉴权、授权片段限量与审计留痕。
 */
@RestController
@RequestMapping("/rag")
public class RagSearchController
{
    private static final Logger log = LoggerFactory.getLogger(RagSearchController.class);

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
     * 获取当前登录用户的知识问答权限摘要。
     *
     * 普通用户前端只展示必要的身份与授权范围信息，
     * 不暴露后台策略表达式和管理配置。
     */
    @GetMapping("/context")
    public AjaxResult context()
    {
        Long userId = SecurityUtils.getLoginUser().getUser().getUserId();
        String userName = SecurityUtils.getUsername();
        Boolean admin = SecurityUtils.isAdmin(userId);

        PermissionContext context = permissionContextService.buildContext(
                userId,
                userName,
                admin
        );

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("userId", context.getUserId());
        result.put("userName", context.getUserName());
        List<String> knowledgeGroups = new ArrayList<String>();
        if (context.getGroupCodes() != null)
        {
            for (String code : context.getGroupCodes())
            {
                if (code != null && !"GROUP_PUBLIC".equalsIgnoreCase(code.trim()))
                {
                    knowledgeGroups.add(code);
                }
            }
        }
        result.put("groupCodes", knowledgeGroups);

        return AjaxResult.success(result);
    }

    /** 普通问答只返回业务结果；管理端保留诊断与审计能力。 */
    @PostMapping("/search")
    public AjaxResult search(@RequestBody RagSearchRequest request)
    {
        boolean diagnostic = SecurityUtils.isAdmin(SecurityUtils.getLoginUser().getUser().getUserId());
        if (!diagnostic && request != null)
        {
            // 普通用户不能通过请求参数启用管理端的演示检索。
            request.setUseRemote(true);
        }
        try
        {
            AjaxResult response = executeSearch(request);
            return diagnostic ? response : toKnowledgeResponse(response);
        }
        catch (RuntimeException exception)
        {
            if (diagnostic)
            {
                throw exception;
            }
            log.warn("知识检索失败，用户ID：{}", SecurityUtils.getLoginUser().getUser().getUserId(), exception);
            return AjaxResult.error("知识服务暂时不可用，请稍后重试或联系管理员。");
        }
    }

    @SuppressWarnings("unchecked")
    private AjaxResult toKnowledgeResponse(AjaxResult response)
    {
        if (Integer.valueOf(503).equals(response.get(AjaxResult.CODE_TAG)))
        {
            return AjaxResult.error(503, "知识服务暂时不可用，请稍后重试或联系管理员。");
        }
        Map<String, Object> source = (Map<String, Object>) response.get(AjaxResult.DATA_TAG);
        boolean allowed = source != null && Boolean.TRUE.equals(source.get("allowAccess"));
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> documents = new ArrayList<Map<String, Object>>();
        if (allowed && source.get("filteredResults") instanceof List)
        {
            for (RagSearchResult item : (List<RagSearchResult>) source.get("filteredResults"))
            {
                Map<String, Object> document = new LinkedHashMap<String, Object>();
                document.put("docId", item.getDocId());
                document.put("title", item.getTitle());
                document.put("content", item.getContent());
                document.put("level", item.getLevel());
                documents.add(document);
            }
        }
        data.put("allowAccess", allowed);
        data.put("filteredResults", documents);
        data.put("filteredResultCount", documents.size());
        data.put("costTime", source == null ? 0L : source.get("costTime"));
        String answer = "当前请求暂时无法完成，请稍后重试或联系管理员。";
        if (allowed)
        {
            answer = Boolean.TRUE.equals(source.get("answerEnabled"))
                    ? String.valueOf(source.get("answer") == null ? "" : source.get("answer"))
                    : documents.isEmpty() ? "未找到可供当前账号使用的相关资料。"
                    : "已找到相关资料，请查看下方的授权引用内容。";
        }
        data.put("answer", answer);
        return new AjaxResult((Integer) response.get(AjaxResult.CODE_TAG),
                allowed ? "操作成功" : "当前请求暂时无法完成，请稍后重试或联系管理员。", data);
    }

    private AjaxResult executeSearch(RagSearchRequest request)
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
        String clientIp = IpUtils.getIpAddr(ServletUtils.getRequest());

        SecureQueryContext secureQueryContext = secureQueryContextService.buildContext(
                userId,
                userName,
                admin,
                request == null ? null : request.getQuery(),
                request == null ? null : request.getTopK(),
                clientIp
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

        // 策略拒绝时立即结束，不检索、不返回任何文档正文。
        if (!Boolean.TRUE.equals(decision.getAllowAccess()))
        {
            List<RagSearchResult> empty = new ArrayList<RagSearchResult>();
            Map<String, Object> denied = new LinkedHashMap<String, Object>();
            denied.put("query", request.getQuery());
            denied.put("allowAccess", false);
            denied.put("searchMode", "blocked_by_policy");
            denied.put("denyReasons", decision.getDenyReasons());
            denied.put("rawResultCount", 0);
            denied.put("filteredResultCount", 0);
            denied.put("rejectedResultCount", 0);
            denied.put("rawResults", empty);
            denied.put("filteredResults", empty);
            denied.put("rejectedResults", empty);
            long deniedCost = System.currentTimeMillis() - startTime;
            denied.put("costTime", deniedCost);
            AjaxResult ajax = AjaxResult.error("请求被权限策略拦截");
            ajax.put("data", denied);
            recordAuditLog(request, context, decision, secureQueryContext,
                    deniedCost, empty, empty, empty, ajax);
            return ajax;
        }

        List<RagSearchResult> rawResults = new ArrayList<RagSearchResult>();
        List<RagSearchResult> qualifiedResults = new ArrayList<RagSearchResult>();
        List<RagSearchResult> rejectedResults = new ArrayList<RagSearchResult>();
        List<RagSearchResult> filteredResults = new ArrayList<RagSearchResult>();
        int candidateLimit = RagSearchLimits.candidateLimit(secureQueryContext.getSafeTopK());
        String answer = "";
        String answerModel = "";
        long answerCostTime = 0L;
        String failureStage = "RETRIEVAL";
        try
        {
            rawResults = Boolean.TRUE.equals(request.getUseRemote())
                    ? ragRemoteSearchService.search(request, context, decision)
                    : ragDocMockSearchService.search(request.getQuery());
            rawResults = limitResults(rawResults, candidateLimit);
            failureStage = "DOCUMENT_AUTHORIZATION";
            qualifiedResults = ragSecondFilterService.filter(context, rawResults);
            rejectedResults = buildRejectedResults(rawResults, qualifiedResults);
            filteredResults = limitResults(qualifiedResults, secureQueryContext.getSafeTopK());

            failureStage = "ANSWER_GENERATION";
            long answerStartTime = System.currentTimeMillis();
            answerModel = ragAnswerService.getModelName();
            answer = ragAnswerService.generateAnswer(request.getQuery(), filteredResults);
            answerCostTime = System.currentTimeMillis() - answerStartTime;
        }
        catch (RuntimeException exception)
        {
            log.warn("知识请求执行失败，用户ID：{}，阶段：{}，异常类型：{}",
                    userId, failureStage, exception.getClass().getSimpleName());
            Map<String, Object> failed = new LinkedHashMap<String, Object>();
            failed.put("allowAccess", true);
            failed.put("executionStatus", "FAILED");
            failed.put("failureStage", failureStage);
            long failureCost = System.currentTimeMillis() - startTime;
            failed.put("costTime", failureCost);
            AjaxResult response = new AjaxResult(503,
                    "知识服务暂时不可用，请稍后重试或联系管理员。", failed);
            recordAuditLog(request, context, decision, secureQueryContext,
                    failureCost, rawResults, qualifiedResults, rejectedResults, response);
            return response;
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

        result.put("candidateTopK", candidateLimit);
        result.put("qualifiedResultCount", qualifiedResults.size());
        result.put("rawResultCount", rawResults.size());
        result.put("filteredResultCount", filteredResults == null ? 0 : filteredResults.size());
        result.put("rejectedResultCount", rejectedResults == null ? 0 : rejectedResults.size());
        result.put("rawResults", new ArrayList<RagSearchResult>());
        result.put("filteredResults", filteredResults);
        result.put("rejectedResults", new ArrayList<RagSearchResult>());
        result.put("costTime", costTime);
        result.put("answer", answer);
        result.put("answerModel", answerModel);
        result.put("answerCostTime", answerCostTime);
        result.put("answerEnabled", ragAnswerService.isEnabled());

        if (Boolean.FALSE.equals(decision.getAllowAccess()))
        {
            AjaxResult ajax = AjaxResult.error("请求被权限策略拦截");
            ajax.put("data", result);
            recordAuditLog(request, context, decision, secureQueryContext, costTime, rawResults, qualifiedResults, rejectedResults, ajax);
            return ajax;
        }

        result.put("message", Boolean.TRUE.equals(request.getUseRemote())
                ? "已完成真实检索、文档权限校验与审计记录"
                : "已完成元数据演示检索、文档权限校验与审计记录");

        AjaxResult ajax = AjaxResult.success(result);
        recordAuditLog(request, context, decision, secureQueryContext, costTime, rawResults, qualifiedResults, rejectedResults, ajax);
        return ajax;
    }

    /** 保持相关性顺序，分别限制候选处理量与授权返回量。 */
    private List<RagSearchResult> limitResults(List<RagSearchResult> results, Integer safeTopK)
    {
        if (results == null || results.isEmpty())
        {
            return new ArrayList<RagSearchResult>();
        }

        int limit = safeTopK == null || safeTopK <= 0 ? 5 : safeTopK;
        if (results.size() <= limit)
        {
            return results;
        }

        return new ArrayList<RagSearchResult>(results.subList(0, limit));
    }

    /** 按片段对象区分拦截结果，避免同一文档的多个片段相互覆盖。 */
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
                    if (raw == filtered)
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
                    raw.setFilterReason("文档片段未通过平台权限校验");
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
        // 响应记录与当前账号实际收到的字段一致；内部诊断保存在独立审计字段。
        Object clientResponse = responseObject;
        if (!Boolean.TRUE.equals(context.getAdmin()) && responseObject instanceof AjaxResult)
        {
            clientResponse = toKnowledgeResponse((AjaxResult) responseObject);
        }
        auditLog.setResponseJson(JSON.toJSONString(clientResponse));
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
