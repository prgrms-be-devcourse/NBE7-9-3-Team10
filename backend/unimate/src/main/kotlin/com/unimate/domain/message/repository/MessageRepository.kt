package com.unimate.domain.message.repository

import com.unimate.domain.message.entity.Message
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MessageRepository : JpaRepository<Message, Long> {

    fun findTopByChatroom_IdOrderByIdDesc(chatroomId: Long): Optional<Message>

    fun countByChatroom_Id(chatroomId: Long): Long
    fun countByChatroom_IdAndIdGreaterThan(chatroomId: Long, lastReadId: Long): Long

    fun countByChatroom_IdAndIdGreaterThanAndSenderId(chatroomId: Long, messageId: Long, senderId: Long): Long
    fun countByChatroom_IdAndSenderId(chatroomId: Long, senderId: Long): Long

    fun findByChatroom_IdOrderByIdDesc(chatroomId: Long, pageable: Pageable): List<Message>
    fun findByChatroom_IdAndIdLessThanOrderByIdDesc(chatroomId: Long, cursorMessageId: Long, pageable: Pageable): List<Message>

    fun existsByIdAndChatroom_Id(id: Long, chatroomId: Long): Boolean

    fun findByChatroomId(chatroomId: Long, pageable: Pageable): Page<Message>

    fun findByChatroom_IdAndSenderIdAndClientMessageId(
        chatroomId: Long,
        senderId: Long,
        clientMessageId: String
    ): Optional<Message>
}