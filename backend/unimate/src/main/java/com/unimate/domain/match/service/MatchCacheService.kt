package com.unimate.domain.match.service

import com.unimate.domain.match.dto.CachedUserProfile
import com.unimate.domain.userProfile.repository.UserProfileRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MatchCacheService(
    private val userProfileRepository: UserProfileRepository
) {
    private val log = LoggerFactory.getLogger(MatchCacheService::class.java)

    @Autowired
    @Lazy
    private lateinit var self: MatchCacheService

    // 애플리케이션 시작 시 캐시 예열
    @EventListener(ApplicationReadyEvent::class)
    fun warmupCache() {
        try {
            self.evictAllCandidatesCache()
            log.info("🧹 기존 캐시 삭제 완료")

            log.info("🔥 캐시 예열 시작")
            val startTime = System.currentTimeMillis()
            val candidates = self.getAllCandidates()
            val duration = System.currentTimeMillis() - startTime
            log.info("캐시 예열 완료 - {}명 로드 ({}ms 소요)", candidates.size, duration)
        } catch (e: Exception) {
            log.warn("캐시 예열 실패: {}", e.message)
        }
    }

    // 전체 후보 목록 조회 (10분 캐시)
    @Cacheable(value = ["matchCandidatesV2"], key = "'all'")
    fun getAllCandidates(): List<CachedUserProfile> {
        log.info("Cache Miss - DB에서 전체 프로필 조회")

        return userProfileRepository.findAll()
            .map { CachedUserProfile.from(it) }
    }

    // 개별 프로필 조회 (1시간 캐시)
    @Cacheable(value = ["userProfile"], key = "#userId")
    fun getUserProfileById(userId: Long): CachedUserProfile? {
        log.info("Cache Miss - DB에서 개별 프로필 조회 (userId: {})", userId)

        return userProfileRepository.findByUserId(userId)
            .map { CachedUserProfile.from(it) }
            .orElse(null)
    }

    // 전체 캐시 무효화
    @CacheEvict(value = ["matchCandidatesV2"], allEntries = true)
    fun evictAllCandidatesCache() {
        log.info("전체 매칭 후보 캐시 삭제")
    }

    // 특정 유저 캐시 무효화
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

    // 여러 유저 캐시 일괄 무효화
    fun evictMultipleUserProfiles(userIds: List<Long>) {
        log.info("다중 유저 프로필 캐시 삭제 ({} 명)", userIds.size)
        userIds.forEach { evictUserProfileCache(it) }
    }
}

