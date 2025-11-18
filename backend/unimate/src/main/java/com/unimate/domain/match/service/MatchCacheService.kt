package com.unimate.domain.match.service

import com.unimate.domain.match.dto.CachedUserProfile
import com.unimate.domain.userProfile.repository.UserProfileRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.system.measureTimeMillis

@Service
@Transactional(readOnly = true)
class MatchCacheService(
    private val userProfileRepository: UserProfileRepository,
    @Lazy private val self: MatchCacheService
) {

    companion object {
        private val log = LoggerFactory.getLogger(MatchCacheService::class.java)
    }

    data class CacheWarmupReport(
        val candidateCount: Int,
        val durationMillis: Long
    )

    @EventListener(ApplicationReadyEvent::class)
    fun warmupCache() {
        refreshCandidateCache()
            .onSuccess { report ->
                log.info("기존 캐시 삭제 완료")
                log.info("캐시 예열 완료 - {}명 로드 ({}ms 소요)", report.candidateCount, report.durationMillis)
            }
            .onFailure { e ->
                log.warn("캐시 예열 실패: {}", e.message)
            }
    }

    
    // 캐시를 강제로 새로고침하고 결과를 재사용할 수 있는 Report 형태로 반환 
    fun refreshCandidateCache(): Result<CacheWarmupReport> = runCatching {
        self.evictAllCandidatesCache()
        log.info("캐시 예열 시작")

        var candidates: List<CachedUserProfile> = emptyList()
        val duration = measureTimeMillis { candidates = self.getAllCandidates() }

        CacheWarmupReport(
            candidateCount = candidates.size,
            durationMillis = duration
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

    @CacheEvict(
        value = ["userProfile", "matchCandidatesV2"],
        key = "#userId",
        allEntries = false,
        beforeInvocation = false
    )
    fun evictUserProfileCache(userId: Long) {
        log.info("유저 프로필 캐시 삭제 (userId: {})", userId)
        evictAllCandidatesCache()
    }

    fun evictMultipleUserProfiles(userIds: List<Long>) {
        val distinctIds = userIds.distinct()
        if (distinctIds.isEmpty()) {
            log.info("다중 유저 프로필 캐시 삭제 요청 - 비어있는 목록, 작업 건너뜀")
            return
        }

        log.info("다중 유저 프로필 캐시 삭제 ({} 명, 중복 제거 후)", distinctIds.size)
        distinctIds.forEach(self::evictUserProfileCache)
    }
}
