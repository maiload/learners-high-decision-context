package com.example.opa.policydecisionlog.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Policy Decision Log API")
                        .description("OPA Decision Log 수집 및 조회 API")
                        .version("1.0.0"))
                .tags(List.of(
                        new Tag().name("Decision Log Ingest").description("Decision Log 수집 API (Command)"),
                        new Tag().name("Decision Log Query").description("Decision Log 조회 API (Query)")
                ));
    }
}
