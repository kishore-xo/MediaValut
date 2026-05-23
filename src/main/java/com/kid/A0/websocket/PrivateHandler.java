package com.kid.A0.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PrivateHandler implements WebSocketHandler {

    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, SimpleMessageListenerContainer> listenerContainerMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final DirectExchange directExchange;
    private final AmqpAdmin amqpAdmin;
    private final ConnectionFactory connectionFactory;

    public PrivateHandler(ObjectMapper objectMapper, RabbitTemplate rabbitTemplate, DirectExchange directExchange, AmqpAdmin amqpAdmin, ConnectionFactory connectionFactory) {
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.directExchange = directExchange;
        this.amqpAdmin = amqpAdmin;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = session.getAttributes().get("username").toString();
        log.info("conneceted to private {}", username);

        sessionMap.put(username, session);
        Queue queue = new Queue(username, true, false, false);
        amqpAdmin.declareQueue(queue);

        Binding binding = BindingBuilder
                .bind(queue)
                .to(directExchange)
                .with(username);
        amqpAdmin.declareBinding(binding);

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(queue.getName());
        container.setMessageListener(message -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message.getBody()));
                }
            } catch (Exception e) {
                log.info("Failed to send message");
            }
        });
        listenerContainerMap.put(username, container);
        container.start();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws IOException {
        String username = session.getAttributes().get("username").toString();
        sessionMap.remove(username);
        SimpleMessageListenerContainer container = listenerContainerMap.remove(username);
        if (container != null) {
            container.stop();
            container.shutdown();
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            ChatMessage chatMessage = objectMapper.readValue(message.getPayload().toString(), ChatMessage.class);
            if (!validatePayload(session, chatMessage)) {
                return;
            }

            if (chatMessage.getTimestamp() == null) {
                chatMessage.setTimestamp(System.currentTimeMillis());
            }
            rabbitTemplate.convertAndSend(directExchange.getName(), chatMessage.getTo(), chatMessage);
        } catch (Exception e) {
            log.error("Failed to send message: ", e);
            sendError(session, "PARSE_ERROR", "Invalid message format");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            sessionMap.remove(username, session);
        }
        log.warn("Private websocket transport error: {}", exception.getMessage());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private boolean validatePayload(WebSocketSession session, ChatMessage payload) {
        if (payload == null) {
            sendError(session, "MISSING_PAYLOAD", "Message payload is required");
            return false;
        }
        if (isBlank(payload.getTo()) || isBlank(payload.getFrom()) || payload.getType() == null) {
            sendError(session, "MISSING_FIELDS", "Fields 'to', 'from', and 'type' are required");
            return false;
        }

        String authenticatedUser = (String) session.getAttributes().get("username");
        if (authenticatedUser == null || !authenticatedUser.equals(payload.getFrom())) {
            log.warn("Sender identity mismatch: Authenticated as '{}' but payload says '{}'", authenticatedUser, payload.getFrom());
            sendError(session, "IDENTITY_MISMATCH", String.format("Sender identity mismatch: Authenticated as '%s' but payload says '%s'", authenticatedUser, payload.getFrom()));
            return false;
        }

        if (payload.getType() == ChatMessage.MessageType.TEXT) {
            if (isBlank(payload.getContent())) {
                sendError(session, "MISSING_CONTENT", "Field 'content' is required for TEXT");
                return false;
            }
            return true;
        }

        if (isBlank(payload.getMediaUrl())) {
            sendError(session, "MISSING_MEDIA", "Field 'mediaUrl' is required for IMAGE/VIDEO");
            return false;
        }
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void sendError(WebSocketSession session, String code, String message) {
        try {
            if (session != null && session.isOpen()) {
                String errorJson = objectMapper.writeValueAsString(Map.of(
                        "type", "ERROR",
                        "code", code,
                        "message", message
                ));
                session.sendMessage(new TextMessage(errorJson));
            }
        } catch (Exception e) {
            // ignore secondary transport errors while reporting failures
        }
    }

}
