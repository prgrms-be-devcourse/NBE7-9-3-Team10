package com.unimate.domain.message.controller

import com.unimate.domain.message.dto.MessageSendRequest
import com.unimate.domain.message.dto.MessageSendResponse
import com.unimate.domain.message.service.MessageService
import com.unimate.global.jwt.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/chatrooms/{chatroomId}/messages")
@Validated
@Tag(name = "MessageController", description = "채팅방 메세지 API")
@SecurityRequirement(name = "BearerAuth")
class MessageController(
    private val messageService: MessageService
) {

    /**
     * 메시지 전송 (REST 보조)
     * POST /api/v1/chatrooms/{chatroomId}/messages
     */
    @PostMapping
    @Operation(summary = "채팅방 메시지 전송")
    fun send(
        @AuthenticationPrincipal me: CustomUserPrincipal,
        @PathVariable chatroomId: Long,
        @Valid @RequestBody req: MessageSendRequest
    ): ResponseEntity<MessageSendResponse> {
        val res = messageService.sendText(
            me.userId, chatroomId, req.content, req.clientMessageId
        )
        return ResponseEntity.ok(res)
    }
}