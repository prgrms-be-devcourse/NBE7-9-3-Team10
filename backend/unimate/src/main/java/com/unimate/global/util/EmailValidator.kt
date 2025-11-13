package com.unimate.global.util

fun isSchoolEmail(email: String): Boolean {
    return email.endsWith(".ac.kr")
}
