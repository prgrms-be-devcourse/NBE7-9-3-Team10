package com.unimate.domain.notification.controller

import com.unimate.domain.notification.entity.Notification
import com.unimate.domain.notification.service.NotificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "NotificationController", description = "알림 API")
@SecurityRequirement(name = "BearerAuth")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping
    @Operation(summary = "알림 목록 조회")
    fun getNotifications(
        authentication: Authentication,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<Notification>> {
        val userId = authentication.name.toLong()
        val notifications = notificationService.getUserNotifications(userId, pageable)
        return ResponseEntity.ok(notifications)
    }

    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 알림 개수 조회")
    fun getUnreadCount(authentication: Authentication): ResponseEntity<Long> {
        val userId = authentication.name.toLong()
        val unreadCount = notificationService.getUnreadCount(userId)
        return ResponseEntity.ok(unreadCount)
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "알림 읽음 처리")
    fun markAsRead(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val userId = authentication.name.toLong()
        notificationService.markAsRead(id, userId)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "알림 삭제")
    fun deleteNotification(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val userId = authentication.name.toLong()
        notificationService.deleteNotification(id, userId)
        return ResponseEntity.ok().build()
    }
}