package com.fufu.ragserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * RAG文件处理独立服务启动类
 *
 * @author fufu
 * @date 2026-05-12
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.fufu.ragserver.mapper")
public class RagServerApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(RagServerApplication.class, args);
    }
}
