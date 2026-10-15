package com.ruoyi.web.controller.rag;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.mapper.AccessDecisionMapper;
import com.ruoyi.system.service.IPermissionContextService;

/** 当前登录人员的只读权限画像；不替代实际检索准入和逐文档鉴权。 */
@RestController
@RequestMapping("/rag/permission/context")
public class RagPermissionContextController
{
    @Autowired
    private IPermissionContextService permissionContextService;

    @Autowired
    private AccessDecisionMapper accessDecisionMapper;

    @PreAuthorize("@ss.hasPermi('rag:permissionContext:view')")
    @GetMapping("/current")
    public AjaxResult current()
    {
        Long userId = SecurityUtils.getLoginUser().getUser().getUserId();
        String userName = SecurityUtils.getUsername();
        Boolean admin = Long.valueOf(1L).equals(userId);
        PermissionContext context = permissionContextService.buildContext(userId, userName, admin);
        Map<String, Object> attributes = accessDecisionMapper.selectUserSecurityAttr(userId);

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("userId", context.getUserId());
        data.put("userName", context.getUserName());
        data.put("admin", context.getAdmin());
        data.put("groups", context.getGroups());
        data.put("policies", context.getPolicies());
        data.put("groupCodes", context.getGroupCodes());
        data.put("scopeCodes", context.getScopeCodes());
        data.put("requestTime", context.getRequestTime());
        data.put("allowAccess", context.getAllowAccess());
        data.put("denyReasons", context.getDenyReasons());

        // 仅返回当前人员的必要展示字段，不返回身份证、手机号等身份资料。
        // 属性缺失时明确返回 null，不把缺失密级当成 PUBLIC。
        data.put("securityAttributesPresent", attributes != null);
        data.put("userSecretLevel", attribute(attributes, "secret_level"));
        data.put("userAccessStatus", attribute(attributes, "access_status"));
        data.put("accessStartTime", attribute(attributes, "access_start_time"));
        data.put("accessEndTime", attribute(attributes, "access_end_time"));
        return AjaxResult.success(data);
    }

    private String attribute(Map<String, Object> attributes, String key)
    {
        Object value = attributes == null ? null : attributes.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
