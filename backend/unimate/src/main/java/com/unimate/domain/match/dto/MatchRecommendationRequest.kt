package com.unimate.domain.match.dto

import jakarta.validation.constraints.Pattern
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

data class MatchRecommendationRequest(
    @field:Pattern(
        regexp = "^(very_early|early|normal|late|very_late)$",
        message = "수면 패턴은 very_early, early, normal, late, very_late 중 하나여야 합니다."
    )
    var sleepPattern: String? = null,

    @field:Pattern(
        regexp = "^(20-22|23-25|26-28|29-30|31\\+)$",
        message = "나이대는 20-22, 23-25, 26-28, 29-30, 31+ 중 하나여야 합니다"
    )
    var ageRange: String? = null,

    @field:Pattern(
        regexp = "^(rarely|monthly|weekly|several_times_weekly|daily)$",
        message = "청소 빈도는 rarely, monthly, weekly, several_times_weekly, daily 중 하나여야 합니다."
    )
    var cleaningFrequency: String? = null,

    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    var startDate: LocalDate? = null,

    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    var endDate: LocalDate? = null
)