package com.school.leave.config;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 出站 HTTP 超时：连接 5s / 读 60s。
 * 防止自定义 endpoint 不可达时 AI 接口长时间挂起（超时后统一走 5001 降级）。
 */
@Configuration
public class AiHttpConfig {

    @Bean
    public RestClientCustomizer aiRestClientTimeout() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(60));
        return builder -> builder.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings));
    }
}
