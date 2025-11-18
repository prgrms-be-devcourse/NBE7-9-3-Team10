package com.unimate.domain.chatroom.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class ChatReadRequest(
    @field:NotNull
    @field:Positive
    val lastReadMessageId: Long
)