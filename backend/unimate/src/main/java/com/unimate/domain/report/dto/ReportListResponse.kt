package com.unimate.domain.report.dto

data class ReportListResponse(
    val content: List<ReportSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
