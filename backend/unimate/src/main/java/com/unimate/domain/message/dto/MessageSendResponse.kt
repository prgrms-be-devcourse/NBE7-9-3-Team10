package com.unimate.domain.message.dto

data class MessageSendResponse(
    val messageId: Long?,
    val chatroomId: Long,
    val senderId: Long,
    val content: String,
    val createdAt: String? // ISO_LOCAL_DATE_TIME
)