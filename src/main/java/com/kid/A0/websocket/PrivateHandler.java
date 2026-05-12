package com.kid.A0.websocket;

import com.kid.A0.websocket.rabbitMQ.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PrivateHandler implements WebSocketHandler {

    private final Map<String, SimpleMessageListenerContainer> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final TopicExchange topicExchange;
    private final ConnectionFactory connectionFactory;

    public PrivateHandler(ObjectMapper objectMapper, RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin, TopicExchange topicExchange, ConnectionFactory connectionFactory) {
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
        this.topicExchange = topicExchange;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = (String) session.getAttributes().get("username");
//        if (username != null && !username.isBlank()) {
//            sessions.put(username, session);
//            log.info("Connected to Private: {}", username);
//        }
        Queue queue = new Queue(username, true);
        amqpAdmin.declareQueue(queue);

        Binding binding = BindingBuilder.bind(queue)
                .to(topicExchange)
                .with(username);
        amqpAdmin.declareBinding(binding);

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setQueueNames(username);
        container.setConnectionFactory(connectionFactory);
        container.setMessageListener(message -> {
            try {
                session.sendMessage(new TextMessage(new String(message.getBody())));
            } catch (IOException e) {
                log.error("Fail to push message");
            }
        });
        container.start();
        sessions.put(username, container);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
//        String username = (String) session.getAttributes().get("username");
//        if (username != null) {
//            sessions.remove(username, session);
//            log.info("Disconnected By Private: {}", username);
//        }
        String username = session.getAttributes().get("username").toString();

        SimpleMessageListenerContainer container = sessions.remove(username);
        if (container != null) {
            container.stop();
            log.info("Continer stop");
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        ChatMessage payload = objectMapper.readValue(message.getPayload().toString(), ChatMessage.class);
        validatePayload(session, payload);

//        WebSocketSession targetSession = sessions.get(payload.getTo());
//        if (targetSession != null && targetSession.isOpen()) {
//            if (payload.getTimestamp() == null) {
//                payload.setTimestamp(System.currentTimeMillis());
//            }
//            targetSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));

        rabbitTemplate.convertAndSend(RabbitMQConfig.topicExchangeName, payload.getTo(), payload);
        return;
//        }

////        log.info("Target user is offline: {}", payload.getTo());
//        sendError(session, "USER_OFFLINE", "Target user is offline");
    }

    private void validatePayload(WebSocketSession session, ChatMessage payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Message payload is required");
        }
        if (isBlank(payload.getTo()) || isBlank(payload.getFrom()) || payload.getType() == null) {
            throw new IllegalArgumentException("Fields 'to', 'from', and 'type' are required");
        }

        String authenticatedUser = (String) session.getAttributes().get("username");
        if (authenticatedUser == null || !authenticatedUser.equals(payload.getFrom())) {
            throw new IllegalArgumentException(String.format("Sender identity mismatch: Authenticated as '%s' but payload says '%s'", authenticatedUser, payload.getFrom()));
        }

        if (payload.getType() == ChatMessage.MessageType.TEXT) {
            if (isBlank(payload.getContent())) {
                throw new IllegalArgumentException("Field 'content' is required for TEXT");
            }
            return;
        }

        if (isBlank(payload.getMediaUrl())) {
            throw new IllegalArgumentException("Field 'mediaUrl' is required for IMAGE/VIDEO");
        }
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

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            sessions.remove(username, session);
        }
        log.warn("Private websocket transport error: {}", exception.getMessage());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

}
