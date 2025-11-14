package com.unimate.global.springDoc

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "UniMate API 서버",
        version = "beta",
        description = """
                        UniMate 서버 API 문서입니다.
                        <br>
                        🔑 JWT 인증이 필요한 API를 테스트하려면 상단의 <b>Authorize</b> 버튼을 눌러
                        '{토큰}' 형식으로 JWT를 입력하세요.
                        """
    ),
    security = [SecurityRequirement(name = "BearerAuth")]
)
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT 토큰을 '{token}' 형식으로 입력하세요."
)
class SpringDoc {
    @Bean
    fun groupApiV1(): GroupedOpenApi? {
        return GroupedOpenApi.builder()
            .group("apiV1")
            .pathsToMatch("/api/v1/**")
            .build()
    }

    @Bean
    fun groupController(): GroupedOpenApi? {
        return GroupedOpenApi.builder()
            .group("home")
            .pathsToExclude("/api/**")
            .build()
    }
}
