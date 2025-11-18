package com.unimate.domain.report.dto

import com.unimate.domain.report.entity.ReportStatus
import java.time.LocalDateTime

data class ReportResponse (
    val reportId : Long,
    val reporterEmail : String?,
    val reportedEmail : String,
    val category : String,
    val content : String,
    val reportStatus : ReportStatus,
    val createdAt : LocalDateTime?,
    val updatedAt : LocalDateTime?
)