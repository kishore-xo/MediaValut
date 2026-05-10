package com.kid.A0.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PrivateHandler implements WebSocketHandler {
    private final Map<String, WebSocketSession> sessions =
            new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public PrivateHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            sessions.put(username, session);
            log.info("Connected to Private: {}", username);

        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        String username = session.getAttributes().get("username").toString();
        if (username != null) {
            log.info("Disconnected By Private: {}", username);
            sessions.remove(username);
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {

        JsonNode json = objectMapper.readTree(message.getPayload().toString());
        String targetId = json.get("to").asString(null);
        WebSocketSession targetSession = sessions.get(targetId);

        if (targetSession != null && targetSession.isOpen()) {
            if (!targetSession.getId().equals(session.getId())) {
                targetSession.sendMessage(message);
            } else {
                log.info("User {} tried to send a message to their own current session.", session.getId());
            }
        } else{
            log.info("TargetUser is Offline: {}", targetId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {

    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

}
