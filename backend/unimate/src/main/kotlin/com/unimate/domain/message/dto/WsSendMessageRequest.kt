package com.unimate.domain.message.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class WsSendMessageRequest(
    @field:NotNull
    val chatroomId: Long,

    @field:NotBlank
    val clientMessageId: String, // 멱등키

    @field:NotNull
    val type: MessageType,

    @field:NotBlank
    val content: String
)