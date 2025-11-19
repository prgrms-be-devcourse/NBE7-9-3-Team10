package com.unimate.domain.message.service

import com.unimate.domain.chatroom.entity.Chatroom
import com.unimate.domain.chatroom.service.ChatroomService
import com.unimate.domain.message.dto.MessageSendResponse
import com.unimate.domain.message.entity.Message
import com.unimate.domain.message.repository.MessageRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class MessageService(
    private val messageRepository: MessageRepository,
    private val chatroomService: ChatroomService
) {

    companion object {
        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    /**
     * REST 방식 전송(멱등 보장)
     */
    @Transactional
    fun sendText(me: Long, chatroomId: Long, content: String, clientMessageId: String): MessageSendResponse {
        val room: Chatroom = chatroomService.validateWritable(me, chatroomId)

        val existing = messageRepository.findByChatroom_IdAndSenderIdAndClientMessageId(chatroomId, me, clientMessageId)
        if (existing.isPresent) {
            val message = existing.get()
            return MessageSendResponse(
                messageId = message.id,
                chatroomId = chatroomId,
                senderId = me,
                content = message.content,
                createdAt = message.createdAt?.format(ISO)
            )
        }

        return try {
            val saved = messageRepository.save(
                Message(
                    chatroom = room,
                    senderId = me,
                    content = content,
                    clientMessageId = clientMessageId
                )
            )
            room.bumpLastMessage(saved.id!!, saved.createdAt!!)
            MessageSendResponse(
                messageId = saved.id,
                chatroomId = chatroomId,
                senderId = me,
                content = saved.content,
                createdAt = saved.createdAt?.format(ISO)
            )
        } catch (dup: DataIntegrityViolationException) {
            val message = messageRepository
                .findByChatroom_IdAndSenderIdAndClientMessageId(chatroomId, me, clientMessageId)
                .orElseThrow { dup }

            MessageSendResponse(
                messageId = message.id,
                chatroomId = chatroomId,
                senderId = me,
                content = message.content,
                createdAt = message.createdAt?.format(ISO)
            )
        }
    }
}