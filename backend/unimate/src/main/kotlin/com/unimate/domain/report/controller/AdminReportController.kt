package com.unimate.domain.report.controller

import com.unimate.domain.report.dto.AdminReportActionRequest
import com.unimate.domain.report.dto.AdminReportActionResponse
import com.unimate.domain.report.dto.ReportDetailResponse
import com.unimate.domain.report.dto.ReportListResponse
import com.unimate.domain.report.service.AdminReportService
import com.unimate.global.jwt.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/reports")
@Tag(name = "AdminReportController", description = "신고 관리자 API")
@SecurityRequirement(name = "BearerAuth")
class AdminReportController(private val adminReportService: AdminReportService) {

    @GetMapping
    @Operation(summary = "신고 조회")
    fun getReports(
        @AuthenticationPrincipal user: CustomUserPrincipal,
        @PageableDefault(size = 10, sort = ["createdAt"]) pageable: Pageable,
        @RequestParam status: String?,
        @RequestParam keyword: String?
    ): ResponseEntity<ReportListResponse> {
        val response = adminReportService.getReports(user.userId, pageable, status, keyword)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "신고 상세 조회")
    fun getReportDetail(
        @AuthenticationPrincipal user: CustomUserPrincipal,
        @PathVariable reportId: Long
    ): ResponseEntity<ReportDetailResponse> {
        val response = adminReportService.getReportDetail(user.userId, reportId)
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{reportId}/action")
    @Operation(summary = "신고 상태 변경")
    fun handleReportAction(
        @AuthenticationPrincipal user: CustomUserPrincipal,
        @PathVariable reportId: Long,
        @Valid @RequestBody request: AdminReportActionRequest
    ): ResponseEntity<AdminReportActionResponse> {
        val response = adminReportService.processReportAction(user.userId, reportId, request)
        return ResponseEntity.ok(response)
    }
}