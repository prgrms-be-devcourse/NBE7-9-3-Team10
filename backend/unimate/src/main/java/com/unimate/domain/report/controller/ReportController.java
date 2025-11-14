package com.unimate.domain.report.controller;

import com.unimate.domain.report.dto.ReportCreateRequest;
import com.unimate.domain.report.service.ReportService;
import com.unimate.global.jwt.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
@Tag(name = "ReportController", description = "신고 API")
@SecurityRequirement(name = "BearerAuth")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "신고 생성")
    public ResponseEntity<ReportResponse> create(
            @Valid @RequestBody ReportCreateRequest rq,
            @AuthenticationPrincipal CustomUserPrincipal user
    ){
        ReportResponse res = reportService.create(user.getEmail(), rq);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

}
