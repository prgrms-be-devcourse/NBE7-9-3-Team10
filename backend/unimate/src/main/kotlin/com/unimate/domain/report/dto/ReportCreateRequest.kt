package com.unimate.domain.report.dto

data class ReportCreateRequest(
    val reportedEmail: String,
    val category: String,
    val content: String
)
