package com.unimate.global.auth.service

import com.unimate.global.auth.dto.Tokens
import com.unimate.global.auth.entity.RefreshToken
import com.unimate.global.auth.model.SubjectType
import com.unimate.global.auth.repository.RefreshTokenRepository
import com.unimate.global.exception.ServiceException
import com.unimate.global.jwt.JwtProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TokenService(
    private val jwtProvider: JwtProvider,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    @Transactional
    fun issueTokens(subjectType: SubjectType, subjectId: Long, email: String): Tokens {
        val token = jwtProvider.generateToken(email, subjectId)

        val existingToken = refreshTokenRepository.findBySubjectTypeAndSubjectId(subjectType, subjectId)
        if (existingToken != null) {
            existingToken.updateToken(email, token.refreshToken)
        } else {
            refreshTokenRepository.save(
                RefreshToken(
                    subjectType,
                    subjectId,
                    email,
                    token.refreshToken
                )
            )
        }

        return Tokens(subjectId, email, token.accessToken, token.refreshToken)
    }

    @Transactional(readOnly = true)
    fun reissueAccessToken(refreshToken: String): String {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw ServiceException.unauthorized("유효하지 않은 리프레시 토큰입니다.")
        }

        val stored = refreshTokenRepository.findByRefreshToken(refreshToken)
            ?: throw ServiceException.notFound("저장된 리프레시 토큰이 없습니다.")

        val newToken = jwtProvider.generateToken(stored.email, stored.subjectId)
        return newToken.accessToken
    }

    @Transactional
    fun logout(refreshToken: String) {
        val rt = refreshTokenRepository.findByRefreshToken(refreshToken)
            ?: throw ServiceException.badRequest("유효하지 않은 리프레시 토큰입니다.")
        refreshTokenRepository.delete(rt)
    }
}