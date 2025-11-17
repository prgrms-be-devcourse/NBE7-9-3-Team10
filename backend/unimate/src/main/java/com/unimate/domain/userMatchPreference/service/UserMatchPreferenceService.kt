package com.unimate.domain.userMatchPreference.service

import com.unimate.domain.match.service.MatchCacheService
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.userMatchPreference.dto.MatchPreferenceRequest
import com.unimate.domain.userMatchPreference.dto.MatchPreferenceResponse
import com.unimate.domain.userMatchPreference.entity.UserMatchPreference
import com.unimate.domain.userMatchPreference.repository.UserMatchPreferenceRepository
import com.unimate.domain.userProfile.repository.UserProfileRepository
import com.unimate.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserMatchPreferenceService(
    private val userMatchPreferenceRepository: UserMatchPreferenceRepository,
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val matchCacheService: MatchCacheService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun updateMyMatchPreferences(userId: Long, requestDto: MatchPreferenceRequest): MatchPreferenceResponse {
        // 사용자 조회
        val user = userRepository.findById(userId)
            .orElseThrow { ServiceException.notFound("사용자를 찾을 수 없습니다.") }

        // 기존 선호도 정보 조회 및 업데이트/생성
        val preference = userMatchPreferenceRepository.findByUserId(userId)
            .orElse(null)

        val updatedPreference = if (preference != null) {
            preference.update(requestDto)
            preference
        } else {
            val newPref = UserMatchPreference.fromDto(user, requestDto)
            userMatchPreferenceRepository.save(newPref)
        }

        // matchingEnabled 필드 true로 설정
        val userProfile = userProfileRepository.findByUserId(userId)
            .orElseThrow { ServiceException.notFound("해당 사용자의 프로필을 찾을 수 없습니다.") }

        userProfile.updateMatchingStatus(true)

        // 캐시 무효화
        matchCacheService.evictAllCandidatesCache()
        log.info("매칭 선호도 등록/수정 - 전체 후보자 캐시 무효화 (userId: {})", userId)

        // responseDto로 변환하여 반환
        return MatchPreferenceResponse.fromEntity(updatedPreference)
    }
}
