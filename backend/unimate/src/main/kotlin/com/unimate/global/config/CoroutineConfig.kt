package com.unimate.global.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineConfig {
    
    // 애플리케이션 전역에서 사용할 코루틴 스코프 생성 (한 작업 실패가 다른 작업에 영향을 주지 않도록 SupervisorJob 사용)
    @Bean
    fun applicationCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

