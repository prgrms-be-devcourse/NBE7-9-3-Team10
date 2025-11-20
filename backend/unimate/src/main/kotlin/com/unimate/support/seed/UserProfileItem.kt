package com.unimate.support.seed

import java.time.LocalDate

data class UserProfileItem(
    val email: String,
    val password: String,
    val name: String,
    val gender: String,
    val birthDate: LocalDate,
    val studentVerified: Boolean,
    val university: String,
    val sleepTime: Int,
    val isPetAllowed: Boolean,
    val isSmoker: Boolean,
    val cleaningFrequency: Int?,
    val preferredAgeGap: Int?,
    val hygieneLevel: Int?,
    val isSnoring: Boolean?,
    val drinkingFrequency: Int?,
    val noiseSensitivity: Int?,
    val guestFrequency: Int?,
    val mbti: String?,
    val startUseDate: LocalDate?,
    val endUseDate: LocalDate?,
    val matchingEnabled: Boolean?
)

