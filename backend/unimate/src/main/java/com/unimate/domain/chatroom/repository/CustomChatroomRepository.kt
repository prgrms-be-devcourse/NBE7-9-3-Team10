package com.unimate.domain.chatroom.repository

import com.unimate.domain.chatroom.entity.Chatroom
import com.unimate.domain.chatroom.entity.ChatroomStatus
import java.time.LocalDateTime

interface CustomChatroomRepository {
    // lastMessageAt(없으면 createdAt) 기준 keyset(커서) 페이징
    fun findRoomsByUserWithCursor(
        userId: Long,
        status: ChatroomStatus?,
        cursor: LocalDateTime?,
        limit: Int
    ): List<Chatroom>
}
