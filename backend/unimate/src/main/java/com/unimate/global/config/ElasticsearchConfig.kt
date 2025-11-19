package com.unimate.global.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

@Configuration
@EnableElasticsearchRepositories(basePackages = ["com.unimate.domain"])
class ElasticsearchConfig {
    // Spring Boot에 Elasticsearch 관련 기능을 활성화하여 Bean 정상 생성 보장
}
