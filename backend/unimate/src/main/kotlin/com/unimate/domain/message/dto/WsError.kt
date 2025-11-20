package com.unimate.domain.message.dto

data class WsError(
    val code: String,
    val message: String, // 사용자 메시지
    val detail: String?
)