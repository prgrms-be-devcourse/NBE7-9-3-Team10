package com.unimate.domain.message.ws

import com.unimate.domain.chatroom.service.ChatroomService
import com.unimate.domain.chatroom.service.UserSessionService
import com.unimate.domain.message.dto.MessageType
import com.unimate.domain.message.dto.WsMessagePush
import com.unimate.domain.message.dto.WsSendAckResponse
import com.unimate.domain.message.dto.WsSendMessageRequest
import com.unimate.domain.message.entity.Message
import com.unimate.domain.message.repository.MessageRepository
import com.unimate.domain.notification.entity.NotificationType
import com.unimate.domain.notification.service.NotificationService
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.global.jwt.CustomUserPrincipal
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import java.security.Principal
import java.time.format.DateTimeFormatter
import java.util.Optional

@Controller
@Tag(name = "ChatWsController", description = "채팅방 WebSocket API")
class ChatWsController(
    private val chatroomService: ChatroomService,
    private val messageRepository: MessageRepository,
    private val messagingTemplate: SimpMessageSendingOperations,
    private val notificationService: NotificationService,
    private val userRepository: UserRepository,
    private val userSessionService: UserSessionService
) {

    companion object {
        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    private val log = LoggerFactory.getLogger(javaClass)

    @MessageMapping("/chat.send")
    fun sendMessage(
        @Payload req: WsSendMessageRequest,
        principal: Optional<Principal>,
        sha: SimpMessageHeaderAccessor?
    ) {
        val user = resolvePrincipal(principal.orElse(null), sha)
            ?: throw AccessDeniedException("인증되지 않은 사용자입니다.")

        val userId = user.userId
        val userNameKey = user.name

        try {
            val room = chatroomService.validateWritable(userId, req.chatroomId)

            var message = messageRepository
                .findByChatroom_IdAndSenderIdAndClientMessageId(req.chatroomId, userId, req.clientMessageId)
                .orElse(null)

            if (message == null) {
                message = try {
                    messageRepository.save(
                        Message(
                            chatroom = room,
                            senderId = userId,
                            content = req.content,
                            clientMessageId = req.clientMessageId
                        )
                    ).also {
                        room.bumpLastMessage(it.id!!, it.createdAt!!)
                    }
                } catch (dup: DataIntegrityViolationException) {
                    messageRepository
                        .findByChatroom_IdAndSenderIdAndClientMessageId(req.chatroomId, userId, req.clientMessageId)
                        .orElseThrow { dup }
                }
            }

            val timestamp = message.createdAt?.format(ISO)

            val push = WsMessagePush(
                messageId = message.id,
                chatroomId = room.id,
                senderId = userId,
                type = MessageType.TEXT,
                content = message.content,
                createdAt = timestamp ?: ""
            )
            messagingTemplate.convertAndSend("/sub/chatroom.${room.id}", push)

            val ack = WsSendAckResponse(
                clientMessageId = req.clientMessageId,
                messageId = message.id,
                status = "OK",
                createdAt = timestamp ?: ""
            )
            messagingTemplate.convertAndSendToUser(userNameKey, "/queue/chat.ack", ack)

            val partnerId = if (room.user1Id == userId) room.user2Id else room.user1Id
            val partner = partnerId ?: return
            val chatroomId = room.id ?: return

            if (!userSessionService.isUserInChatroom(partner, chatroomId)) {
                val sender = userRepository.findById(userId)
                    .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다.") }

                notificationService.createChatNotification(
                    userId = partnerId,
                    type = NotificationType.CHAT,
                    message = "${sender.name} 님에게 새로운 메시지가 도착했습니다.",
                    senderName = sender.name,
                    senderId = userId,
                    chatroomId = room.id
                )
            }
        } catch (ex: Exception) {
            log.error(
                "Failed to send message: chatroomId={}, senderId={}, clientMessageId={}, error={}",
                req.chatroomId,
                userId,
                req.clientMessageId,
                ex.message,
                ex
            )
            throw ex
        }
    }

    private fun resolvePrincipal(
        principal: Principal?,
        sha: SimpMessageHeaderAccessor?
    ): CustomUserPrincipal? {
        val direct = (principal as? Authentication)?.principal as? CustomUserPrincipal
        if (direct != null) return direct

        return (sha?.user as? Authentication)?.principal as? CustomUserPrincipal
    }
}