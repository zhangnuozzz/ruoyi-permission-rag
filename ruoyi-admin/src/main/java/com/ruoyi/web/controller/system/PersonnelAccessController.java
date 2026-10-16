package com.ruoyi.web.controller.system;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.system.domain.PersonnelSaveRequest;
import com.ruoyi.system.service.IPersonnelAccessService;

/**
 * 人员资料、密级及知悉范围的统一维护入口。
 */
@RestController
@RequestMapping("/system/personnelAccess")
public class PersonnelAccessController extends BaseController
{
    @Autowired
    private IPersonnelAccessService personnelAccessService;

    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/{userId}")
    public AjaxResult getProfile(@PathVariable Long userId)
    {
        return AjaxResult.success(personnelAccessService.getProfile(userId));
    }

    @PreAuthorize("@ss.hasPermi('system:user:add') and @ss.hasPermi('system:user:edit')")
    @Log(title = "人员资料与权限", businessType = BusinessType.INSERT, isSaveRequestData = false)
    @PostMapping
    public AjaxResult create(@Validated @RequestBody PersonnelSaveRequest request)
    {
        return AjaxResult.success(personnelAccessService.createPersonnel(request));
    }

    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "人员资料与权限", businessType = BusinessType.UPDATE, isSaveRequestData = false)
    @PutMapping("/save/{userId}")
    public AjaxResult update(@PathVariable Long userId, @Validated @RequestBody PersonnelSaveRequest request)
    {
        personnelAccessService.updatePersonnel(userId, request);
        return AjaxResult.success();
    }

    /**
     * 保留仅维护访问配置的原接口，复用同一套业务规则。
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "人员权限配置", businessType = BusinessType.UPDATE, isSaveRequestData = false)
    @PutMapping("/{userId}")
    public AjaxResult saveProfile(@PathVariable Long userId, @RequestBody Map<String, Object> access)
    {
        personnelAccessService.saveAccess(userId, access);
        return AjaxResult.success();
    }

    /**
     * 业务异常从事务代理返回后已回滚，与网络中断等结果未知的情况分别反馈。
     */
    @ExceptionHandler(CustomException.class)
    public AjaxResult handleBusinessException(CustomException exception)
    {
        AjaxResult result = AjaxResult.error(exception.getCode() == null ? 500 : exception.getCode(),
                exception.getMessage());
        result.put("saveStatus", "REJECTED");
        return result;
    }

    @ExceptionHandler({ BindException.class, MethodArgumentNotValidException.class })
    public AjaxResult handleValidationException(Exception exception)
    {
        BindingResult errors = exception instanceof BindException
                ? ((BindException) exception).getBindingResult()
                : ((MethodArgumentNotValidException) exception).getBindingResult();
        String message = errors.hasErrors() ? errors.getAllErrors().get(0).getDefaultMessage()
                : "人员资料格式无效，请核对后重试";
        return handleBusinessException(new CustomException(message));
    }

}
