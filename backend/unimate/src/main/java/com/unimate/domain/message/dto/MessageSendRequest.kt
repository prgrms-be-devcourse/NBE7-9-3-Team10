package com.unimate.domain.message.dto

import jakarta.validation.constraints.NotBlank

data class MessageSendRequest(
    @field:NotBlank
    val content: String,

    @field:NotBlank
    val clientMessageId: String // 멱등키
)