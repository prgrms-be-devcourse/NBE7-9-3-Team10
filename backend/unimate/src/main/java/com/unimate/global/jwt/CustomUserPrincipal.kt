package com.unimate.global.jwt

import java.security.Principal

//Principal 인터페이스 구현 추가 -> STOMP user-destination 라우팅 키로 getName()을 사용하기 위함
class CustomUserPrincipal(
    @JvmField val userId: Long,
    @JvmField val email: String
) : Principal {

    override fun getName(): String = userId.toString()

    override fun toString(): String = name

    //getter룰 위해 임시 작성
    fun getUserId(): Long = userId
    //getter룰 위해 임시 작성
    fun getEmail(): String = email
}
