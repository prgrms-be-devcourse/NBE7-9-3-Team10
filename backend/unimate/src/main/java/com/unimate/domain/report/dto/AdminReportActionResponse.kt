package com.unimate.domain.report.dto;

data class AdminReportActionResponse(
    val reportId: Long,
    val newReportStatus: String,
    val message: String
)
