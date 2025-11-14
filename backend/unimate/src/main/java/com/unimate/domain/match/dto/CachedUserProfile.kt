package com.unimate.domain.match.dto

import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.userProfile.entity.UserProfile
import java.io.Serializable
import java.time.LocalDate

data class CachedUserProfile(
    // user
    val userId: Long,
    val name: String,
    val email: String,
    val gender: Gender,
    val birthDate: LocalDate,
    val university: String,
    val studentVerified: Boolean,

    // userProfile
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
    val matchingEnabled: Boolean
) : Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        fun from(profile: UserProfile): CachedUserProfile {
            val user: User = profile.user

            return CachedUserProfile(
                userId = requireNotNull(user.id),
                name = user.name,
                email = user.email,
                gender = user.gender,
                birthDate = user.birthDate,
                university = user.university,
                studentVerified = user.studentVerified,

                sleepTime = profile.sleepTime,
                isPetAllowed = profile.isPetAllowed,
                isSmoker = profile.isSmoker,
                cleaningFrequency = profile.cleaningFrequency,
                preferredAgeGap = profile.preferredAgeGap,
                hygieneLevel = profile.hygieneLevel,
                isSnoring = profile.isSnoring,
                drinkingFrequency = profile.drinkingFrequency,
                noiseSensitivity = profile.noiseSensitivity,
                guestFrequency = profile.guestFrequency,
                mbti = profile.mbti,
                startUseDate = profile.startUseDate,
                endUseDate = profile.endUseDate,
                matchingEnabled = profile.matchingEnabled
            )
        }
    }
}
