package com.unimate.domain.chatroom.service

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class UserSessionService {

    private val userActiveChatrooms = ConcurrentHashMap<Long, Long>()

    fun enterChatroom(userId: Long, chatroomId: Long) {
        userActiveChatrooms[userId] = chatroomId
    }

    fun leaveChatroom(userId: Long, chatroomId: Long) {
        val currentChatroomId = userActiveChatrooms[userId]
        if (currentChatroomId != null && currentChatroomId == chatroomId) {
            userActiveChatrooms.remove(userId)
        }
    }

    fun isUserInChatroom(userId: Long, chatroomId: Long): Boolean {
        val currentChatroomId = userActiveChatrooms[userId]
        return chatroomId == currentChatroomId
    }
}