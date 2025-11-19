package com.unimate.domain.chatroom.service

import ChatRoomListResponse
import com.unimate.domain.chatroom.dto.*
import com.unimate.domain.chatroom.entity.Chatroom
import com.unimate.domain.chatroom.entity.ChatroomStatus
import com.unimate.domain.chatroom.repository.ChatroomRepository
import com.unimate.domain.match.repository.MatchRepository
import com.unimate.domain.message.entity.Message
import com.unimate.domain.message.repository.MessageRepository
import com.unimate.global.exception.ServiceException
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.userBlock.service.UserBlockService
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class ChatroomService(
    private val chatroomRepository: ChatroomRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val userSessionService: UserSessionService,
    private val matchRepository: MatchRepository,
    private val userBlockService: UserBlockService
) {

    companion object {
        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    private fun getRoomOrThrow(chatroomId: Long): Chatroom =
        chatroomRepository.findById(chatroomId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다.")
            }

    private fun assertMember(me: Long?, room: Chatroom) {
        if (me == null || (me != room.user1Id && me != room.user2Id)) {
            throw AccessDeniedException("채팅방에 참여 중인 사용자가 아닙니다.")
        }
    }

    private fun partnerIdOf(me: Long, room: Chatroom): Long =
        if (me == room.user1Id) room.user2Id else room.user1Id

    private fun coalesceLastAt(room: Chatroom): LocalDateTime =
        room.lastMessageAt ?: room.createdAt ?: LocalDateTime.now()

    @Transactional
    fun createIfNotExists(me: Long?, partnerId: Long?): ChatRoomCreateResponse {
        if (me == null || partnerId == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 사용자 ID입니다.")
        }
        if (me == partnerId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신과의 채팅은 불가능합니다.")
        }

        val smaller = minOf(me, partnerId)
        val larger = maxOf(me, partnerId)

        val room = chatroomRepository
            .findBySmallerUserIdAndLargerUserId(smaller, larger)
            .map { existing ->
                if (existing.status == ChatroomStatus.CLOSED ||
                    existing.user1Status == ChatroomStatus.CLOSED ||
                    existing.user2Status == ChatroomStatus.CLOSED
                ) {
                    existing.reactivate()
                    chatroomRepository.save(existing)
                } else {
                    existing
                }
            }
            .orElseGet { chatroomRepository.save(Chatroom.create(me, partnerId)) }

        return ChatRoomCreateResponse(
            chatroomId = requireNotNull(room.id),
            user1Id = room.user1Id,
            user2Id = room.user2Id,
            status = room.status.name,
            createdAt = ISO.format(requireNotNull(room.createdAt))
        )
    }

    fun getDetail(me: Long?, chatroomId: Long): ChatRoomDetailResponse {
        val room = getRoomOrThrow(chatroomId)
        assertMember(me, room)

        if (me != null) {
            userSessionService.enterChatroom(me, chatroomId)
        }

        val partnerId = partnerIdOf(me!!, room)
        val isPartnerDeleted = !userRepository.existsById(partnerId)

        val partnerName: String
        val partnerUniversity: String

        if (isPartnerDeleted) {
            partnerName = "탈퇴한 사용자"
            partnerUniversity = ""
        } else {
            val partner = userRepository.findById(partnerId)
            partnerName = partner.map { it.name }.orElse("알 수 없는 사용자")
            partnerUniversity = partner.map { it.university }.orElse("")
        }

        // 차단 여부 확인 추가!
        val isBlocked = userBlockService.isBlocked(me, partnerId)  // 내가 상대방을 차단했는지
        val isBlockedByPartner = userBlockService.isBlocked(partnerId, me)  // 상대방이 나를 차단했는지

        return ChatRoomDetailResponse(
            chatroomId = room.id,
            user1Id = room.user1Id,
            user2Id = room.user2Id,
            partnerName = partnerName,
            partnerUniversity = partnerUniversity,
            isPartnerDeleted = isPartnerDeleted,
            isBlocked = isBlocked,
            isBlockedByPartner = isBlockedByPartner,
            status = room.status.name,
            user1Status = room.user1Status.name,
            user2Status = room.user2Status.name,
            createdAt = ISO.format(room.createdAt),
            updatedAt = ISO.format(room.updatedAt),
            lastReadMessageIdUser1 = room.lastReadMessageIdUser1,
            lastReadMessageIdUser2 = room.lastReadMessageIdUser2
        )
    }

    fun listMyRooms(
        me: Long,
        cursor: String?,
        limit: Int,
        status: ChatroomStatus?
    ): ChatRoomListResponse {
        val finalStatus = status ?: ChatroomStatus.ACTIVE

        var cursorAt: LocalDateTime? = null
        if (!cursor.isNullOrBlank()) {
            cursorAt = runCatching { LocalDateTime.parse(cursor, ISO) }.getOrNull()
        }

        val rooms = chatroomRepository.findRoomsByUserWithCursor(me, finalStatus, cursorAt, limit)

        val items = rooms.map { room ->
            val partnerId = partnerIdOf(me, room)
            val partnerName = userRepository.findById(partnerId)
                .map { it.name }
                .orElse("알 수 없는 사용자")

            val lastSummary: ChatRoomListResponse.LastMessageSummary? =
                room.lastMessageId?.let { lastMsgId ->
                    val message: Message? = messageRepository.findById(lastMsgId).orElse(null)
                    when {
                        message != null && message.createdAt != null -> {
                            ChatRoomListResponse.LastMessageSummary(
                                messageId = message.id,
                                content = message.content,
                                createdAt = ISO.format(message.createdAt)
                            )
                        }
                        room.lastMessageAt != null -> {
                            ChatRoomListResponse.LastMessageSummary(
                                messageId = lastMsgId,
                                content = null,
                                createdAt = ISO.format(room.lastMessageAt)
                            )
                        }
                        else -> null
                    }
                }

            val roomId = requireNotNull(room.id)
            val partner = requireNotNull(partnerId)
            val myLastRead = if (me == room.user1Id) room.lastReadMessageIdUser1 else room.lastReadMessageIdUser2

            val unreadCount = if (myLastRead != null) {
                messageRepository.countByChatroom_IdAndIdGreaterThanAndSenderId(roomId, myLastRead, partner)
            } else {
                messageRepository.countByChatroom_IdAndSenderId(roomId, partner)
            }

            // 차단 여부 확인 추가
            val isBlocked = userBlockService.isBlocked(me, partnerId)

            ChatRoomListResponse.ChatRoomListItem(
                chatroomId = room.id,
                partnerId = partnerId,
                partnerName = partnerName,
                lastMessage = lastSummary,
                unreadCount = unreadCount,
                isBlocked = isBlocked,
                status = room.status.name,
                updatedAt = ISO.format(room.updatedAt)
            )
        }

        val nextCursorOut = if (rooms.isNotEmpty() && rooms.size == limit) {
            ISO.format(coalesceLastAt(rooms.last()))
        } else {
            null
        }

        return ChatRoomListResponse(items, nextCursorOut)
    }

    fun getHistory(me: Long, chatroomId: Long, beforeMessageId: Long?, limit: Int): ChatHistoryResponse {
        val room = getRoomOrThrow(chatroomId)
        assertMember(me, room)

        val pageable = PageRequest.of(0, limit)
        val messages: List<Message> = if (beforeMessageId == null) {
            messageRepository.findByChatroom_IdOrderByIdDesc(chatroomId, pageable)
        } else {
            if (!messageRepository.existsByIdAndChatroom_Id(beforeMessageId, chatroomId)) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid cursor")
            }
            messageRepository.findByChatroom_IdAndIdLessThanOrderByIdDesc(chatroomId, beforeMessageId, pageable)
        }

        // 차단해도 이전 채팅 내용은 볼 수 있도록 필터링하지 않음
        val items = messages.map { m ->
            ChatHistoryResponse.ChatMessageItem(
                messageId = m.id,
                chatroomId = m.chatroom.id,
                senderId = m.senderId,
                content = m.content,
                createdAt = ISO.format(m.createdAt)
            )
        }

        val nextCursor = messages.lastOrNull()?.id?.toString()
        return ChatHistoryResponse(items, nextCursor)
    }

    @Transactional
    fun updateLastRead(me: Long, chatroomId: Long, lastReadMessageId: Long?): ChatReadResponse {
        val room = getRoomOrThrow(chatroomId)
        assertMember(me, room)

        if (lastReadMessageId != null &&
            !messageRepository.existsByIdAndChatroom_Id(lastReadMessageId, chatroomId)
        ) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 메시지 ID입니다.")
        }

        room.updateLastRead(me, lastReadMessageId)
        val now = ISO.format(LocalDateTime.now())

        return ChatReadResponse(
            chatroomId = chatroomId,
            userId = me,
            lastReadMessageId = lastReadMessageId,
            updatedAt = now
        )
    }

    @Transactional
    fun leave(me: Long, chatroomId: Long): ChatRoomLeaveResponse {
        val room = getRoomOrThrow(chatroomId)
        assertMember(me, room)

        room.leave(me)

        val partnerId = partnerIdOf(me, room)
        matchRepository.findMatchBetweenUsers(me, partnerId)
            ?.let { matchRepository.delete(it) }

        return ChatRoomLeaveResponse(
            chatroomId = room.id,
            status = room.status.name,
            updatedAt = ISO.format(LocalDateTime.now())
        )
    }

    fun leaveNotification(userId: Long, chatroomId: Long) {
        userSessionService.leaveChatroom(userId, chatroomId)
    }

    fun validateReadable(userId: Long, chatroomId: Long): Chatroom {
        val room = getRoomOrThrow(chatroomId)
        if (room.user1Id != userId && room.user2Id != userId) {
            throw ServiceException.forbidden("해당 채팅방에 대한 권한이 없습니다.")
        }
        return room
    }

    fun validateWritable(senderId: Long, chatroomId: Long): Chatroom {
        val room = getRoomOrThrow(chatroomId)
        assertMember(senderId, room)

        if (senderId == room.user1Id && room.user2Status == ChatroomStatus.CLOSED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 나간 사용자입니다.")
        }
        if (senderId == room.user2Id && room.user1Status == ChatroomStatus.CLOSED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 나간 사용자입니다.")
        }
        if (room.status == ChatroomStatus.CLOSED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "닫힌 채팅방입니다.")
        }

        return room
    }
}