package com.unimate.domain.userProfile.dto

import com.unimate.domain.userProfile.entity.UserProfile
import java.time.LocalDate
import java.time.LocalDateTime

data class ProfileResponse(
    val id: Long,
    val sleepTime: Int,
    val isPetAllowed: Boolean,
    val isSmoker: Boolean,
    val cleaningFrequency: Int,
    val preferredAgeGap: Int,
    val hygieneLevel: Int,
    val isSnoring: Boolean,
    val drinkingFrequency: Int,
    val noiseSensitivity: Int,
    val guestFrequency: Int,
    val mbti: String,
    val startUseDate: LocalDate,
    val endUseDate: LocalDate,
    val matchingEnabled: Boolean,

    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(entity: UserProfile): ProfileResponse =
            ProfileResponse(
                id = entity.id,
                sleepTime = entity.sleepTime,
                isPetAllowed = entity.isPetAllowed,
                isSmoker = entity.isSmoker,
                cleaningFrequency = entity.cleaningFrequency,
                preferredAgeGap = entity.preferredAgeGap,
                hygieneLevel = entity.hygieneLevel,
                isSnoring = entity.isSnoring,
                drinkingFrequency = entity.drinkingFrequency,
                noiseSensitivity = entity.noiseSensitivity,
                guestFrequency = entity.guestFrequency,
                mbti = entity.mbti,
                startUseDate = entity.startUseDate,
                endUseDate = entity.endUseDate,
                matchingEnabled = entity.matchingEnabled,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
    }
}