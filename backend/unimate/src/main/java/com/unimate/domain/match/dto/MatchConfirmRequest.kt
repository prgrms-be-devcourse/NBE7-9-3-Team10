package com.unimate.domain.match.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class MatchConfirmRequest(
    @field:NotBlank(message = "액션은 필수입니다")
    @field:Pattern(
        regexp = "^(accept|reject)$",
        message = "액션은 accept 또는 reject여야 합니다"
    )
    var action: String
)
