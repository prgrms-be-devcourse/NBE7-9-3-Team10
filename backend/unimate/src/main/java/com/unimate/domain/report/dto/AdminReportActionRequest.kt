package com.unimate.domain.report.dto

data class AdminReportActionRequest(
    val action: ActionType
) {
    enum class ActionType {
        REJECT,
        DEACTIVATE
    }
}
