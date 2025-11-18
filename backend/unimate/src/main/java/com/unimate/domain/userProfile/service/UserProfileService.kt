package com.unimate.domain.userProfile.service

import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import com.unimate.domain.match.repository.MatchRepository
import com.unimate.domain.match.service.MatchCacheService
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.userProfile.dto.ProfileCreateRequest
import com.unimate.domain.userProfile.dto.ProfileResponse
import com.unimate.domain.userProfile.entity.UserProfile
import com.unimate.domain.userProfile.repository.UserProfileRepository
import com.unimate.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserProfileService(
    private val userProfileRepository: UserProfileRepository,
    private val userRepository: UserRepository,
    private val matchRepository: MatchRepository,
    private val matchCacheService: MatchCacheService
) {
    private val log = LoggerFactory.getLogger(UserProfileService::class.java)

    @Transactional
    fun create(email: String, req: ProfileCreateRequest): ProfileResponse {
        val userRef = userRepository.findByEmail(email)
            ?: throw ServiceException.notFound("이메일에 해당하는 유저를 찾을 수 없습니다.")

        if (userProfileRepository.existsByUserEmail(email)) {
            throw ServiceException.conflict("이미 등록된 프로필이 존재합니다.")
        }
        
        val profile = UserProfile.fromRequest(userRef, req)
        val saved = userProfileRepository.save(profile)

        saved.id?.let { userId ->
            matchCacheService.evictUserProfileCache(userId)
            log.info("프로필 생성 - 캐시 무효화 (userId: {})", userId)
        }

        return saved.toResponse()
    }

    fun getByEmail(email: String): ProfileResponse {
        val profile = userProfileRepository.findByUserEmail(email)
            .orElseThrow { ServiceException.notFound("사용자 프로필을 찾을 수 없습니다.") }

        return profile.toResponse()
    }

    @Transactional
    fun update(email: String, req: ProfileCreateRequest): ProfileResponse {
        val profile = userProfileRepository.findByUserEmail(email)
            .orElseThrow { ServiceException.notFound("사용자 프로필을 찾을 수 없습니다.") }

        profile.update(req)

        val userId = requireNotNull(profile.user.id) { "User ID가 null입니다." }
        matchCacheService.evictUserProfileCache(userId)
        log.info("프로필 수정 - 캐시 무효화 (userId: {})", userId)

        return profile.toResponse()
    }

    private fun UserProfile.toResponse(): ProfileResponse = ProfileResponse.from(this)

    @Transactional
    fun cancelMatching(userId: Long) {
        val userProfile = userProfileRepository.findByUserId(userId)
            .orElseThrow { ServiceException.notFound("사용자 프로필을 찾을 수 없습니다.") }
        
        userProfile.updateMatchingStatus(false)

        matchRepository.deleteUnconfirmedMatchesByUserId(userId, MatchType.REQUEST, MatchStatus.ACCEPTED)

        matchCacheService.evictUserProfileCache(userId)
        log.info("매칭 비활성화 - 캐시 무효화 (userId: {})", userId)
    }
}
