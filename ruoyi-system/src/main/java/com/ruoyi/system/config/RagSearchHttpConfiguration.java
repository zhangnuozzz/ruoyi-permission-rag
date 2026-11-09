package com.ruoyi.system.config;

import java.util.concurrent.TimeUnit;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** 文件检索服务专用连接池，由容器统一关闭。 */
@Configuration
public class RagSearchHttpConfiguration
{
    @Bean(name = "ragSearchHttpClient", destroyMethod = "close")
    public CloseableHttpClient ragSearchHttpClient()
    {
        PoolingHttpClientConnectionManager manager =
                new PoolingHttpClientConnectionManager(60, TimeUnit.SECONDS);
        manager.setMaxTotal(200);
        manager.setDefaultMaxPerRoute(200);
        manager.setValidateAfterInactivity(2000);
        return HttpClients.custom()
                .setConnectionManager(manager)
                .disableAutomaticRetries()
                .disableCookieManagement()
                .disableRedirectHandling()
                .evictExpiredConnections()
                .evictIdleConnections(30, TimeUnit.SECONDS)
                .build();
    }

    @Bean(name = "ragSearchRestTemplate")
    public RestTemplate ragSearchRestTemplate(
            @Qualifier("ragSearchHttpClient") CloseableHttpClient client)
    {
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(client);
        factory.setConnectTimeout(3000);
        factory.setConnectionRequestTimeout(3000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }
}
