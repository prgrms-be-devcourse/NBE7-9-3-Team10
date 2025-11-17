package com.unimate.domain.chatroom.dto

data class ChatRoomCreateResponse(
    val chatroomId: Long,
    val user1Id: Long,
    val user2Id: Long,
    val status: String,
    val createdAt: String
)