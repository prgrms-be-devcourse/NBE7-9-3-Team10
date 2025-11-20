package com.unimate.domain.verification.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SchoolDomainResponse(
    @JsonProperty("domain")
    val domain: String?
)