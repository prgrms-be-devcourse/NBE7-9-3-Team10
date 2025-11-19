package com.unimate.domain.message.dto

data class WsSendAckResponse(
    val clientMessageId: String,
    val messageId: Long?,
    val status: String, // "OK"
    val createdAt: String
)