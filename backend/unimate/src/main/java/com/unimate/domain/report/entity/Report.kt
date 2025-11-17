package com.unimate.domain.report.entity

import com.unimate.domain.user.user.entity.User
import com.unimate.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "report")
class Report(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporterId", nullable = true)
    var reporter: User?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reportedId", nullable = true)
    var reported: User?,

    var category: String,

    @Lob
    var content: String,

    var reportStatus: ReportStatus = ReportStatus.RECEIVED
) : BaseEntity() {

    fun updateStatus(status: ReportStatus) {
        this.reportStatus = status
    }
}



