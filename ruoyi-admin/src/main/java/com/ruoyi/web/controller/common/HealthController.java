package com.ruoyi.web.controller.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;

/**
 * Basic public endpoint used to verify that the backend is alive.
 */
@RestController
public class HealthController
{
    @GetMapping("/")
    public AjaxResult index()
    {
        return AjaxResult.success("若依后端启动成功");
    }
}
