package com.unimate.global.csv.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SchoolDto(
    @JsonProperty("schoolName")
    val schoolName: String,

    @JsonProperty("homepageAdres")
    val homepageAdres: String
)