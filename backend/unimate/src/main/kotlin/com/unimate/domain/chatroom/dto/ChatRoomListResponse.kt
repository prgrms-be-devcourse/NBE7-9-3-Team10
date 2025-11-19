data class ChatRoomListResponse(
    val items: List<ChatRoomListItem>,
    val nextCursor: String? = null // 커서 페이지네이션(없으면 null)
) {
    data class ChatRoomListItem(
        val chatroomId: Long?,
        val partnerId: Long,
        val partnerName: String, // 상대방 이름
        val lastMessage: LastMessageSummary?,
        val unreadCount: Long,
        val isBlocked: Boolean,
        val status: String,      // ACTIVE | CLOSED
        val updatedAt: String    // ISO_LOCAL_DATE_TIME
    )

    data class LastMessageSummary(
        val messageId: Long?,
        val content: String?,
        val createdAt: String // ISO_LOCAL_DATE_TIME
    )
}