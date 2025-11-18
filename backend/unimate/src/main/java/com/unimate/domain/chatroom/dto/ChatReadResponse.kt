package com.unimate.domain.chatroom.dto

data class ChatReadResponse(
    val chatroomId: Long?,
    val userId: Long?,
    val lastReadMessageId: Long?,
    val updatedAt: String? // ISO_LOCAL_DATE_TIME
)