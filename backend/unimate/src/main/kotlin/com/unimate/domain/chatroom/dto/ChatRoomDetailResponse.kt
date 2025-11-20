package com.unimate.domain.chatroom.dto

data class ChatRoomDetailResponse(
    val chatroomId: Long?,
    val user1Id: Long,
    val user2Id: Long,
    val partnerName: String,
    val partnerUniversity: String,
    val isPartnerDeleted: Boolean,
    val isBlocked: Boolean,              // 내가 상대방을 차단했는지
    val isBlockedByPartner: Boolean,    // 상대방이 나를 차단했는지
    val status: String,              // ACTIVE | CLOSED
    val user1Status: String,
    val user2Status: String,
    val createdAt: String,
    val updatedAt: String,
    val lastReadMessageIdUser1: Long?,
    val lastReadMessageIdUser2: Long?
)