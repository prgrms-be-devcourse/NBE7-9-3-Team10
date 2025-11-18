package com.unimate.domain.match.service

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class CacheScheduler(
    private val matchCacheService: MatchCacheService,
    private val applicationScope: CoroutineScope
) {
    
    companion object {
        private val log = LoggerFactory.getLogger(CacheScheduler::class.java)
    }

    // 5분마다 자동으로 캐시를 갱신하여 최신 데이터 유지 (비동기 실행으로 다른 작업에 영향 없음)
    @Scheduled(fixedRate = 300000) // 5분 = 300,000ms
    fun refreshCacheScheduled() {
        applicationScope.launch {
            matchCacheService.scheduledCacheRefresh()
        }
    }
}

