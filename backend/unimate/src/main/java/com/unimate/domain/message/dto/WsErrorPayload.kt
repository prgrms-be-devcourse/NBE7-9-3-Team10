package com.unimate.domain.message.dto

data class WsErrorPayload(
    val code: String,
    val message: String,
    val detail: String? = null // optional
)