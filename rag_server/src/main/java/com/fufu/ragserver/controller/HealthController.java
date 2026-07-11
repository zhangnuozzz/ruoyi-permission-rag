package com.fufu.ragserver.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import com.fufu.ragserver.domain.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 服务状态入口。
 */
@RestController
public class HealthController
{
    @GetMapping("/")
    public AjaxResult home()
    {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "rag-server");
        status.put("status", "UP");
        status.put("port", 8081);
        return AjaxResult.success(status);
    }
}
