package com.unimate.global.school.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SchoolDomainResponse(
    @JsonProperty("domain")
    val domain: String?
)