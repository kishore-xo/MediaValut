package com.kid.A0.websocket

import com.kid.A0.security.JwtUtil
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import org.springframework.web.util.UriComponentsBuilder

@Component
class MyHandShakeInterceptor(private val jwtUtil: JwtUtil) : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val token = UriComponentsBuilder.fromUri(request.uri)
            .build()
            .queryParams
            .getFirst("token")
            ?.trim()
            ?: return false

        return try {
            val username = jwtUtil.extractUserName(token)
            if (username.isNullOrBlank() || jwtUtil.isTokenExpired(token)) {
                false
            } else {
                attributes["username"] = username
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
        // no-op
    }

}