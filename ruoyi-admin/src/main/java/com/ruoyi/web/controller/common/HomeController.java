package com.ruoyi.web.controller.common;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 后端根地址入口。
 */
@Controller
public class HomeController
{
    @GetMapping("/")
    public String home(HttpServletRequest request)
    {
        // 使用请求中的主机名，兼容 localhost、WSL 地址和局域网地址。
        return "redirect://" + request.getServerName() + ":1024/";
    }
}
