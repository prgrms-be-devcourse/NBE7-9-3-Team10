package com.unimate.domain.report.dto

import java.time.LocalDateTime

data class ReportSummary(
    val reportId: Long,
    val reporterName: String?,
    val reportedName: String?,
    val category: String,
    val status: String,
    val createdAt: LocalDateTime?
)
