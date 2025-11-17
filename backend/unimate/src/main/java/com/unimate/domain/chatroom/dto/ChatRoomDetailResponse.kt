package com.unimate.domain.chatroom.dto

data class ChatRoomDetailResponse(
    val chatroomId: Long?,
    val user1Id: Long,
    val user2Id: Long,
    val partnerName: String,
    val partnerUniversity: String,
    val isPartnerDeleted: Boolean,
    val status: String,              // ACTIVE | CLOSED
    val user1Status: String,
    val user2Status: String,
    val createdAt: String,
    val updatedAt: String,
    val lastReadMessageIdUser1: Long?,
    val lastReadMessageIdUser2: Long?
)