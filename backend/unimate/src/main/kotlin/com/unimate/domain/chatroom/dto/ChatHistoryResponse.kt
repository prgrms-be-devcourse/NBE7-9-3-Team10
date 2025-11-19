package com.unimate.domain.chatroom.dto

data class ChatHistoryResponse(
    val items: List<ChatMessageItem>,
    val nextCursor: String? = null // 커서 없으면 null
) {
    data class ChatMessageItem(
        val messageId: Long?,
        val chatroomId: Long?,
        val senderId: Long,
        val content: String?,   // 텍스트만
        val createdAt: String?  // ISO_LOCAL_DATE_TIME
    )
}