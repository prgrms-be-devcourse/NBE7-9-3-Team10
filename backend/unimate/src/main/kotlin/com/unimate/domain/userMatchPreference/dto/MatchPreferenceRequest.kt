package com.unimate.domain.userMatchPreference.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.unimate.global.validator.ValidDateRange
import jakarta.validation.constraints.*
import java.time.LocalDate

@ValidDateRange(startDate = "startUseDate", endDate = "endUseDate", message = "룸메이트 종료일은 시작일보다 빠를 수 없습니다.")
data class MatchPreferenceRequest(
    @field:NotNull(message = "startUseDate 필드는 null일 수 없습니다.")
    @field:FutureOrPresent(message = "startUseDate는 현재 또는 미래의 날짜여야 합니다.")
    val startUseDate: LocalDate,

    @field:NotNull(message = "endUseDate 필드는 null일 수 없습니다.")
    @field:Future(message = "endUseDate는 미래의 날짜여야 합니다.")
    val endUseDate: LocalDate,

    @field:NotNull(message = "sleepTime 필드는 null일 수 없습니다.")
    @field:Min(value = 1, message = "sleepTime은 1 이상의 값이어야 합니다.")
    @field:Max(value = 5, message = "sleepTime은 5 이하의 값이어야 합니다.")
    val sleepTime: Int,

    @field:NotNull(message = "isPetAllowed 필드는 null일 수 없습니다.")
    @JsonProperty("isPetAllowed")
    val isPetAllowed: Boolean,

    @field:NotNull(message = "isSmoker 필드는 null일 수 없습니다.")
    @JsonProperty("isSmoker")
    val isSmoker: Boolean,

    @field:NotNull(message = "cleaningFrequency 필드는 null일 수 없습니다.")
    @field:Min(value = 1, message = "cleaningFrequency은 1 이상의 값이어야 합니다.")
    @field:Max(value = 5, message = "cleaningFrequency은 5 이하의 값이어야 합니다.")
    val cleaningFrequency: Int,

    @field:NotNull(message = "preferredAgeGap 필드는 null일 수 없습니다.")
    @field:Min(value = 1, message = "preferredAgeGap는 1에서 5 사이의 값이어야 합니다.")
    @field:Max(value = 5, message = "preferredAgeGap는 1에서 5 사이의 값이어야 합니다.")
    val preferredAgeGap: Int,

    @field:NotNull(message = "hygieneLevel 필드는 null일 수 없습니다.")
    @field:Min(value = 1, message = "hygieneLevel은 1 이상의 값이어야 합니다.")
    @field:Max(value = 5, message = "hygieneLevel은 5 이하의 값이어야 합니다.")
    val hygieneLevel: Int,

    @field:NotNull(message = "isSnoring 필드는 null일 수 없습니다.")
    @JsonProperty("isSnoring")
    val isSnoring: Boolean,

    @field:NotNull(message = "drinkingFrequency 필드는 null일 수 없습니다.")
    @field:Min(value = 1, message = "drinkingFrequency은 1 이상의 값이어야 합니다.")
    @field:Max(value = 5, message = "drinkingFrequency은 5 이하의 값이어야 합니다.")
    val drinkingFrequency: Int,

    @field:NotNull(message = "noiseSensitivity 필드는 null일 수 없습니다.")
    @field:Min(value = 1, message = "noiseSensitivity은 1 이상의 값이어야 합니다.")
    @field:Max(value = 5, message = "noiseSensitivity은 5 이하의 값이어야 합니다.")
    val noiseSensitivity: Int,

    @field:NotNull(message = "guestFrequency 필드는 null일 수 없습니다.")
    @field:Min(value = 1, message = "guestFrequency은 1 이상의 값이어야 합니다.")
    @field:Max(value = 5, message = "guestFrequency은 5 이하의 값이어야 합니다.")
    val guestFrequency: Int
)
