package com.unimate.domain.userMatchPreference.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.unimate.domain.userMatchPreference.entity.UserMatchPreference
import java.time.LocalDate
import java.time.LocalDateTime

data class MatchPreferenceResponse(
    val userId: Long,
    val startUseDate: LocalDate,
    val endUseDate: LocalDate,
    val sleepTime: Int,
    @get:JsonProperty("isPetAllowed")
    val isPetAllowed: Boolean,
    @get:JsonProperty("isSmoker")
    val isSmoker: Boolean,
    val cleaningFrequency: Int,
    val preferredAgeGap: Int,
    val hygieneLevel: Int,
    @get:JsonProperty("isSnoring")
    val isSnoring: Boolean,
    val drinkingFrequency: Int,
    val noiseSensitivity: Int,
    val guestFrequency: Int,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromEntity(userMatchPreference: UserMatchPreference): MatchPreferenceResponse {
            return MatchPreferenceResponse(
                userId = userMatchPreference.user.id!!,
                startUseDate = userMatchPreference.startUseDate,
                endUseDate = userMatchPreference.endUseDate,
                sleepTime = userMatchPreference.sleepTime,
                isPetAllowed = userMatchPreference.isPetAllowed,
                isSmoker = userMatchPreference.isSmoker,
                cleaningFrequency = userMatchPreference.cleaningFrequency,
                preferredAgeGap = userMatchPreference.preferredAgeGap,
                hygieneLevel = userMatchPreference.hygieneLevel,
                isSnoring = userMatchPreference.isSnoring,
                drinkingFrequency = userMatchPreference.drinkingFrequency,
                noiseSensitivity = userMatchPreference.noiseSensitivity,
                guestFrequency = userMatchPreference.guestFrequency,
                updatedAt = userMatchPreference.updatedAt!!
            )
        }
    }
}
