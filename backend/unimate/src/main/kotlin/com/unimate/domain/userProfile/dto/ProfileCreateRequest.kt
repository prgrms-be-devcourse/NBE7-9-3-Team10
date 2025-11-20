package com.unimate.domain.userProfile.dto

import com.unimate.domain.userProfile.entity.UserProfile
import java.time.LocalDate

data class ProfileCreateRequest(
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
) {
    companion object {
        fun from(entity: UserProfile): ProfileCreateRequest =
            ProfileCreateRequest(
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
                matchingEnabled = entity.matchingEnabled
            )
    }
}