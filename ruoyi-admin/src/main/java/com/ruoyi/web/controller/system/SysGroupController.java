package com.ruoyi.system.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.SysGroup;
import com.ruoyi.system.service.ISysGroupService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 用户组Controller
 * 
 * @author zhangnuo
 * @date 2026-05-05
 */
@RestController
@RequestMapping("/system/group")
public class SysGroupController extends BaseController
{
    @Autowired
    private ISysGroupService sysGroupService;

    /**
     * 查询用户组列表
     */
    @PreAuthorize("@ss.hasPermi('system:group:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysGroup sysGroup)
    {
        startPage();
        List<SysGroup> list = sysGroupService.selectSysGroupList(sysGroup);
        return getDataTable(list);
    }

    /**
     * 导出用户组列表
     */
    @PreAuthorize("@ss.hasPermi('system:group:export')")
    @Log(title = "用户组", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public AjaxResult export(HttpServletResponse response, SysGroup sysGroup)
    {
        List<SysGroup> list = sysGroupService.selectSysGroupList(sysGroup);
        ExcelUtil<SysGroup> util = new ExcelUtil<SysGroup>(SysGroup.class);
        return util.exportExcel(list, "用户组数据");
    }

    /**
     * 获取用户组详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:group:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(sysGroupService.selectSysGroupById(id));
    }

    /**
     * 新增用户组
     */
    @PreAuthorize("@ss.hasPermi('system:group:add')")
    @Log(title = "用户组", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysGroup sysGroup)
    {
        return toAjax(sysGroupService.insertSysGroup(sysGroup));
    }

    /**
     * 修改用户组
     */
    @PreAuthorize("@ss.hasPermi('system:group:edit')")
    @Log(title = "用户组", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysGroup sysGroup)
    {
        return toAjax(sysGroupService.updateSysGroup(sysGroup));
    }

    /**
     * 删除用户组
     */
    @PreAuthorize("@ss.hasPermi('system:group:remove')")
    @Log(title = "用户组", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(sysGroupService.deleteSysGroupByIds(ids));
    }
}
