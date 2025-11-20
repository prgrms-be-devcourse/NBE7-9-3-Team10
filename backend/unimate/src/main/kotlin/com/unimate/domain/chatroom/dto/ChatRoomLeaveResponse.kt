package com.unimate.domain.chatroom.dto

data class ChatRoomLeaveResponse(
    val chatroomId: Long?,
    val status: String,    // CLOSED 로 전환
    val updatedAt: String  // ISO_LOCAL_DATE_TIME
)