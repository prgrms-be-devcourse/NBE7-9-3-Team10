package com.unimate.global.ws

import com.unimate.global.jwt.CustomUserPrincipal
import com.unimate.global.jwt.JwtProvider
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import java.security.Principal
import java.util.Locale

@Component
class StompAuthChannelInterceptor(
    private val jwtProvider: JwtProvider
) : ChannelInterceptor {

    companion object {
        private const val SESSION_PRINCIPAL_KEY = "WS_PRINCIPAL"
    }

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        val command = accessor.command ?: return message

        if (accessor.user != null) {
            return message
        }

        return if (command == StompCommand.CONNECT) {
            handleConnect(message, accessor)
        } else {
            restorePrincipalFromSession(message, accessor)
        }
    }

    private fun handleConnect(message: Message<*>, accessor: StompHeaderAccessor): Message<*> {
        var token = resolveTokenCaseInsensitive(accessor)?.replace("\\s+".toRegex(), "")

        if (token == null || !jwtProvider.validateToken(token)) {
            throw AccessDeniedException("인증되지 않은 WebSocket 연결입니다.")
        }

        val userId = jwtProvider.getUserIdFromToken(token)
        val email = jwtProvider.getEmailFromToken(token)

        val principal = CustomUserPrincipal(userId, email)
        val auth = UsernamePasswordAuthenticationToken(principal, null, emptyList())

        accessor.user = auth
        accessor.sessionAttributes?.put(SESSION_PRINCIPAL_KEY, auth)

        return MessageBuilder.createMessage(message.payload, accessor.messageHeaders)
    }

    private fun restorePrincipalFromSession(
        message: Message<*>,
        accessor: StompHeaderAccessor
    ): Message<*> {
        val saved = accessor.sessionAttributes
            ?.get(SESSION_PRINCIPAL_KEY) as? Principal

        return if (saved != null) {
            accessor.user = saved
            MessageBuilder.createMessage(message.payload, accessor.messageHeaders)
        } else {
            message
        }
    }

    private fun resolveTokenCaseInsensitive(accessor: StompHeaderAccessor): String? {
        val headers = accessor.toNativeHeaderMap() ?: return null

        firstHeaderIgnoreCase(headers, "Authorization")?.let { bearer ->
            if (bearer.startsWith("Bearer ")) {
                return bearer.substring(7)
            }
        }

        return firstHeaderIgnoreCase(headers, "access-token")?.takeIf { it.isNotBlank() }
    }

    private fun firstHeaderIgnoreCase(
        headers: Map<String, List<String>>,
        key: String
    ): String? {
        val target = key.lowercase(Locale.ROOT)
        headers.entries.forEach { (headerKey, values) ->
            if (headerKey?.lowercase(Locale.ROOT) == target) {
                values?.forEach { value ->
                    if (!value.isNullOrBlank()) return value
                }
            }
        }
        return null
    }
}

