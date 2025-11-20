package com.unimate.domain.chatroom.controller

import ChatRoomListResponse
import com.unimate.domain.chatroom.dto.*
import com.unimate.domain.chatroom.entity.ChatroomStatus
import com.unimate.domain.chatroom.service.ChatroomService
import com.unimate.global.jwt.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/chatrooms")
@Validated
@Tag(name = "ChatroomController", description = "채팅방 API")
@SecurityRequirement(name = "BearerAuth")
class ChatroomController(
    private val chatroomService: ChatroomService
) {

    /** 방 생성(멱등) */
    @PostMapping
    @Operation(summary = "채팅방 생성")
    fun create(
        @AuthenticationPrincipal me: CustomUserPrincipal,
        @Valid @RequestBody req: ChatRoomCreateRequest
    ): ResponseEntity<ChatRoomCreateResponse> {
        val res = chatroomService.createIfNotExists(me.userId, req.partnerId)
        return ResponseEntity.ok(res)
    }

    /** 방 상세 */
    @GetMapping("/{chatroomId}")
    @Operation(summary = "채팅방 상세 조회")
    fun detail(
        @AuthenticationPrincipal me: CustomUserPrincipal,
        @PathVariable chatroomId: Long
    ): ResponseEntity<ChatRoomDetailResponse> {
        val res = chatroomService.getDetail(me.userId, chatroomId)
        return ResponseEntity.ok(res)
    }

    /** 내 방 목록(커서 페이지네이션) */
    @GetMapping
    @Operation(summary = "채팅방 목록 조회")
    fun list(
        @AuthenticationPrincipal me: CustomUserPrincipal,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "30") @Min(1) @Max(100) limit: Int,
        @RequestParam(required = false) status: ChatroomStatus?
    ): ResponseEntity<ChatRoomListResponse> {
        val res = chatroomService.listMyRooms(me.userId, cursor, limit, status)
        return ResponseEntity.ok(res)
    }

    /** 메시지 히스토리 조회 */
    @GetMapping("/{chatroomId}/messages")
    @Operation(summary = "채팅방 메세지 조회")
    fun history(
        @AuthenticationPrincipal me: CustomUserPrincipal,
        @PathVariable chatroomId: Long,
        @RequestParam(defaultValue = "30") @Min(1) @Max(100) limit: Int,
        @RequestParam(required = false) beforeMessageId: Long?
    ): ResponseEntity<ChatHistoryResponse> {
        val res = chatroomService.getHistory(me.userId, chatroomId, beforeMessageId, limit)
        return ResponseEntity.ok(res)
    }

    /** 읽음 처리 */
    @PostMapping("/{chatroomId}/read")
    @Operation(summary = "채팅방 메세지 읽음")
    fun read(
        @AuthenticationPrincipal me: CustomUserPrincipal,
        @PathVariable chatroomId: Long,
        @Valid @RequestBody req: ChatReadRequest
    ): ResponseEntity<ChatReadResponse> {
        val res = chatroomService.updateLastRead(me.userId, chatroomId, req.lastReadMessageId)
        return ResponseEntity.ok(res)
    }

    /** 나가기 */
    @PostMapping("/{chatroomId}/leave")
    @Operation(summary = "채팅방 나가기")
    fun leave(
        @AuthenticationPrincipal me: CustomUserPrincipal,
        @PathVariable chatroomId: Long
    ): ResponseEntity<ChatRoomLeaveResponse> {
        val res = chatroomService.leave(me.userId, chatroomId)
        return ResponseEntity.ok(res)
    }

    /** 채팅방 퇴장 알림 */
    @PostMapping("/{chatroomId}/leave-notification")
    @Operation(summary = "채팅방 퇴장 알림")
    fun leaveNotification(
        @AuthenticationPrincipal me: CustomUserPrincipal,
        @PathVariable chatroomId: Long
    ): ResponseEntity<Void> {
        chatroomService.leaveNotification(me.userId, chatroomId)
        return ResponseEntity.ok().build()
    }


}