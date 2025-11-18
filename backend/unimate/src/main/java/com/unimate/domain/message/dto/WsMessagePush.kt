package com.unimate.domain.message.dto

data class WsMessagePush(
    val messageId: Long?,
    val chatroomId: Long?,
    val senderId: Long,
    val type: MessageType,        // TEXT
    val content: String,
    val createdAt: String         // ISO_LOCAL_DATE_TIME
)