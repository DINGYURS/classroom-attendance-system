package com.project.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / Swagger 配置
 */
@Configuration
public class Knife4jConfig {

    /**
     * 自定义 OpenAPI 信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo());
    }

    /**
     * API 基本信息
     */
    private Info apiInfo() {
        return new Info()
                .title("课堂点名系统 API 文档")
                .version("1.0.0")
                .description("基于人脸检测与识别的课堂快速点名系统后端 API")
                .contact(new Contact()
                        .name("开发者")
                        .email("developer@example.com"));
    }

}
