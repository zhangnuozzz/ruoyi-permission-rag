package com.ruoyi.system.service;

import com.ruoyi.common.utils.ip.IpUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** IP 规则的统一判定入口，白名单只豁免 IP 黑名单，不授予文档权限。 */
@Service
public class IpAccessService
{
    private static final String REQUEST_DECISION = IpAccessService.class.getName() + ".decision";

    private static final class RequestDecision
    {
        private final String address;
        private final Decision decision;

        private RequestDecision(String address, Decision decision)
        {
            this.address = address;
            this.decision = decision;
        }
    }

    public enum Decision
    {
        ALLOW, WHITELIST, BLACKLIST, INVALID
    }

    @Autowired
    private ISysIpWhitelistService whitelistService;

    @Autowired
    private ISysIpBlacklistService blacklistService;

    public Decision evaluate(String address)
    {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes instanceof ServletRequestAttributes
                ? ((ServletRequestAttributes) attributes).getRequest() : null;
        return evaluate(address, request);
    }

    public Decision evaluate(String address, HttpServletRequest request)
    {
        String ip = IpUtils.normalizeAddress(address);
        if (ip == null)
        {
            return Decision.INVALID;
        }
        Object value = request == null ? null : request.getAttribute(REQUEST_DECISION);
        if (value instanceof RequestDecision)
        {
            RequestDecision checked = (RequestDecision) value;
            if (ip.equals(checked.address)) return checked.decision;
        }
        Decision decision = whitelistService.isIpAllowed(ip) ? Decision.WHITELIST
                : (blacklistService.isIpBlocked(ip) ? Decision.BLACKLIST : Decision.ALLOW);
        if (request != null) request.setAttribute(REQUEST_DECISION, new RequestDecision(ip, decision));
        return decision;
    }
}
