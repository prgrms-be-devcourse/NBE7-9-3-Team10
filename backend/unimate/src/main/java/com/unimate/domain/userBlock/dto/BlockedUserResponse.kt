package com.unimate.domain.userBlock.dto

import java.time.LocalDateTime

/**
 * 차단된 사용자 응답 DTO
 */
data class BlockedUserResponse(
    val userId: Long,
    val blockedAt: LocalDateTime
)