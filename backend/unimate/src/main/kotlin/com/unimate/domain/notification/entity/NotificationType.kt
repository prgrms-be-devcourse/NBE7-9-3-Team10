package com.unimate.domain.notification.entity

enum class NotificationType {
    LIKE,        // 좋아요 알림
    CHAT,        // 채팅 알림
    MATCH,       // 매칭 알림
    LIKE_CANCELED, // 좋아요 취소 알림
    REVIEW       // 리뷰 작성 알림
}