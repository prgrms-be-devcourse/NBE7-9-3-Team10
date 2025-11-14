package com.unimate.domain.report.controller;


import com.unimate.domain.report.dto.AdminReportActionResponse;
import com.unimate.domain.report.service.AdminReportService;
import com.unimate.global.jwt.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Tag(name = "AdminReportController", description = "신고 관리자 API")
@SecurityRequirement(name = "BearerAuth")
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping
    @Operation(summary = "신고 조회")
    public ResponseEntity<ReportListResponse> getReports(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        ReportListResponse response = adminReportService.getReports(user.getUserId(), pageable, status, keyword);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "신고 상세 조회")
    public ResponseEntity<ReportDetailResponse> getReportDetail(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @PathVariable Long reportId) {
        ReportDetailResponse response = adminReportService.getReportDetail(user.getUserId(), reportId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{reportId}/action")
    @Operation(summary = "신고 상태 변경")
    public ResponseEntity<AdminReportActionResponse> handleReportAction(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @PathVariable Long reportId,
            @Valid @RequestBody AdminReportActionRequest request
    ) {
        AdminReportActionResponse response = adminReportService.processReportAction(user.getUserId(), reportId, request);
        return ResponseEntity.ok(response);
    }
}
