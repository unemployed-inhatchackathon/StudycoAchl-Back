package com.studycoAchl.hackaton.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("은하톤 해커톤 API")
                        .description("AI 튜터 채팅 시스템 API")
                        .version("v1.0.0"))
                .components(new Components()
                        .addSchemas("UUID", new Schema<>()
                                .type("string")
                                .format("uuid")
                                .example("550e8400-e29b-41d4-a716-446655440000")));
    }
}
