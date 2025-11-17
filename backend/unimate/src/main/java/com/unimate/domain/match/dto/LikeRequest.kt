package com.unimate.domain.match.dto

import jakarta.validation.constraints.NotNull

data class LikeRequest(
    @field:NotNull(message = "좋아요를 받는 사용자 ID는 필수입니다.")
    val receiverId: Long
)