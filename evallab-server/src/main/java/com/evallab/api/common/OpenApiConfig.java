package com.evallab.api.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI evalLabOpenApi() {
        return new OpenAPI().info(new Info()
                .title("EvalLab API")
                .version("0.1.0")
                .description("Agent 离线评测平台：Trace 采集 → 多维评分 → 失败归因 → 回归门禁"));
    }
}
