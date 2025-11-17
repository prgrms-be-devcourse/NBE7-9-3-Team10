package com.unimate.domain.chatroom.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class ChatRoomCreateRequest(
    @field:NotNull
    @field:Positive
    val partnerId: Long // 상대방 ID (me는 토큰에서)
)