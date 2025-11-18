package com.unimate.domain.match.service

import com.unimate.domain.match.dto.CachedUserProfile
import com.unimate.domain.userProfile.repository.UserProfileRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.time.measureTime

@Service
@Transactional(readOnly = true)
class MatchCacheService(
    private val userProfileRepository: UserProfileRepository,
    @Lazy private val self: MatchCacheService,
    private val applicationScope: CoroutineScope
) {

    companion object {
        private val log = LoggerFactory.getLogger(MatchCacheService::class.java)
        private const val MAX_RETRIES = 3 // 재시도 최대 횟수
        private const val RETRY_DELAY_MS = 2000L // 재시도 전 대기 시간 (2초)
    }

    data class CacheWarmupReport(
        val candidateCount: Int,
        val durationMillis: Long,
        val retryCount: Int = 0, // 재시도 횟수
        val estimatedMemoryMB: Double = 0.0 // 예상 메모리 사용량 (MB)
    )

    // 애플리케이션 시작 시 비동기로 캐시 예열 (블로킹 없이 즉시 시작 가능)
    @EventListener(ApplicationReadyEvent::class)
    fun warmupCache() {
        applicationScope.launch {
            try {
                val report = refreshCandidateCacheWithRetry()
                log.info("캐시 예열 완료 - {}명 로드 ({}ms 소요, 재시도: {}회)", 
                    report.candidateCount, report.durationMillis, report.retryCount)
                
                if (report.estimatedMemoryMB > 0) {
                    log.info("예상 메모리 사용량: {} MB", report.estimatedMemoryMB)
                }
            } catch (e: Exception) {
                log.error("캐시 예열 최종 실패", e)
            }
        }
    }

    // 재시도 로직이 포함된 캐시 갱신 (최대 3번까지 자동 재시도)
    suspend fun refreshCandidateCacheWithRetry(
        maxRetries: Int = MAX_RETRIES
    ): CacheWarmupReport = coroutineScope {
        var lastException: Exception? = null
        var retryCount = 0

        repeat(maxRetries) { attempt ->
            try {
                val report = refreshCandidateCacheAsync()
                return@coroutineScope report.copy(retryCount = attempt)
            } catch (e: Exception) {
                lastException = e
                retryCount = attempt + 1
                
                if (attempt < maxRetries - 1) {
                    log.warn("캐시 예열 실패 (재시도 {}/{}): {}", retryCount, maxRetries, e.message)
                    delay(RETRY_DELAY_MS) // 재시도 전 2초 대기
                }
            }
        }

        throw lastException ?: IllegalStateException("캐시 예열 실패")
    }

    // 코루틴을 사용한 비동기 캐시 갱신 (I/O 작업이므로 Dispatchers.IO 사용)
    suspend fun refreshCandidateCacheAsync(): CacheWarmupReport = withContext(Dispatchers.IO) {
        self.evictAllCandidatesCache()
        log.info("캐시 예열 시작")

        val candidates: List<CachedUserProfile>
        val duration = measureTime {
            candidates = self.getAllCandidates()
        }
        
        // 메모리 사용량 추정 (각 CachedUserProfile이 약 1KB라고 가정)
        val estimatedMemory = (candidates.size * 1024) / (1024.0 * 1024.0)
        
        CacheWarmupReport(
            candidateCount = candidates.size,
            durationMillis = duration.inWholeMilliseconds,
            estimatedMemoryMB = estimatedMemory
        )
    }

    @Cacheable(value = ["matchCandidatesV2"], key = "'all'")
    fun getAllCandidates(): List<CachedUserProfile> {
        log.info("Cache Miss - DB에서 전체 프로필 조회")
        return userProfileRepository.findAll()
            .map(CachedUserProfile::from)
    }

    @Cacheable(value = ["userProfile"], key = "#userId")
    fun getUserProfileById(userId: Long): CachedUserProfile? {
        log.info("Cache Miss - DB에서 개별 프로필 조회 (userId: {})", userId)
        return userProfileRepository.findByUserId(userId)
            .map(CachedUserProfile::from)
            .orElse(null)
    }

    @CacheEvict(value = ["matchCandidatesV2"], allEntries = true)
    fun evictAllCandidatesCache() {
        log.info("전체 매칭 후보 캐시 삭제")
    }

    // 개별 사용자 프로필 캐시만 삭제 (확장성을 위해 유지, 전체 캐시는 TTL로 자동 갱신)
    @CacheEvict(
        value = ["userProfile"],
        key = "#userId",
        allEntries = false
    )
    fun evictUserProfileCacheOnly(userId: Long) {
        log.info("개별 유저 프로필 캐시 삭제 (userId: {})", userId)
    }

    // 개별 캐시 + 전체 캐시 모두 삭제 (매칭 결과에 영향을 주는 변경 시 사용)
    fun evictUserProfileCache(userId: Long) {
        self.evictUserProfileCacheOnly(userId)
        self.evictAllCandidatesCache()
        log.info("유저 프로필 캐시 및 전체 후보 캐시 삭제 완료 (userId: {})", userId)
    }

    // 여러 사용자의 캐시를 병렬로 삭제 (코루틴 사용)
    suspend fun evictMultipleUserProfilesAsync(userIds: List<Long>) = coroutineScope {
        val distinctIds = userIds.distinct()
        
        if (distinctIds.isEmpty()) {
            log.info("다중 유저 프로필 캐시 삭제 요청 - 비어있는 목록, 작업 건너뜀")
            return@coroutineScope
        }

        log.info("다중 유저 프로필 캐시 삭제 ({} 명, 중복 제거 후)", distinctIds.size)
        
        // 병렬로 캐시 삭제 실행 (여러 작업을 동시에 처리하여 성능 향상)
        distinctIds.map { userId ->
            async(Dispatchers.IO) {
                self.evictUserProfileCache(userId)
            }
        }.awaitAll()
    }

    // 주기적으로 캐시를 갱신하여 데이터 일관성 유지 (스케줄러에서 호출)
    suspend fun scheduledCacheRefresh() {
        try {
            val report = refreshCandidateCacheAsync()
            log.info("스케줄된 캐시 갱신 완료 - {}명", report.candidateCount)
        } catch (e: Exception) {
            log.warn("스케줄된 캐시 갱신 실패: {}", e.message)
        }
    }
}
