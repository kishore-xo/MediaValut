package com.kid.A0.websocket

import com.kid.A0.security.JwtUtil
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class MyHandShakeInterceptor(private val jwtUtil: JwtUtil) : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val query = request.uri.query ?: return false;
        val token = getId(query) ?: return false;

        try {
            val username = jwtUtil.extractUserName(token);

            if (username != null && !jwtUtil.isTokenExpired(token)) {
                attributes["username"] = username;
                return true;
            }
        } catch (e: kotlin.Exception) {

        }
        return false;
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
    }

    fun getId(query: String): String? {
        if (query.contains("token=")) {
            return query.split("token=")[1].split("&")[0];
        }
        return null;
    }

}