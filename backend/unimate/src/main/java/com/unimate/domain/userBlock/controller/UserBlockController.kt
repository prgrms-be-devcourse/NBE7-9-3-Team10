package com.unimate.domain.userBlock.controller

import com.unimate.domain.userBlock.dto.BlockedUserResponse
import com.unimate.domain.userBlock.service.UserBlockService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "UserBlockController", description = "사용자 차단 API")
@SecurityRequirement(name = "BearerAuth")
class UserBlockController(
    private val userBlockService: UserBlockService
) {

    /**
     * 사용자 차단
     */
    @PostMapping("/{userId}/block")
    @Operation(summary = "사용자 차단")
    fun blockUser(
        @PathVariable userId: Long,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val currentUserId = authentication.name.toLong()
        userBlockService.blockUser(currentUserId, userId)
        return ResponseEntity.ok().build()
    }

    /**
     * 차단 해제
     */
    @DeleteMapping("/{userId}/block")
    @Operation(summary = "차단 해제")
    fun unblockUser(
        @PathVariable userId: Long,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val currentUserId = authentication.name.toLong()
        userBlockService.unblockUser(currentUserId, userId)
        return ResponseEntity.ok().build()
    }

    /**
     * 차단 목록 조회
     */
    @GetMapping("/me/blocked")
    @Operation(summary = "차단 목록 조회")
    fun getBlockedUsers(
        authentication: Authentication
    ): ResponseEntity<List<BlockedUserResponse>> {
        val currentUserId = authentication.name.toLong()
        val blockedUsers = userBlockService.getBlockedUsers(currentUserId)
        val response = blockedUsers.map {
            BlockedUserResponse(
                userId = it.blockedId,
                blockedAt = it.blockedAt
            )
        }
        return ResponseEntity.ok(response)
    }
}

