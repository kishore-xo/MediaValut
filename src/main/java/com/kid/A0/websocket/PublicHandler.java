package com.kid.A0.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class PublicHandler implements WebSocketHandler {

    private static final String PUBLIC_EXCHANGE_NAME = "chat.public.exchange";
    private final Set<WebSocketSession> globalSessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper;
    private final TopicExchange topicExchange;
    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;

    public PublicHandler(ObjectMapper objectMapper, TopicExchange topicExchange, AmqpAdmin amqpAdmin, RabbitTemplate rabbitTemplate) {
        this.objectMapper = objectMapper;
        this.topicExchange = topicExchange;
        this.amqpAdmin = amqpAdmin;
        this.rabbitTemplate = rabbitTemplate;
    }


    @Override

    public void afterConnectionEstablished(WebSocketSession session) {
        String username = session.getAttributes().get("username").toString();
        log.info("Connected to Global chat {}", username);
        globalSessions.add(session);

    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            ChatMessage chatMessage = objectMapper.readValue(message.getPayload().toString(), ChatMessage.class);

            if (chatMessage.getTimestamp() == null) {
                chatMessage.setTimestamp(System.currentTimeMillis());
            }
            rabbitTemplate.convertAndSend(topicExchange.getName(), "global", chatMessage);
        } catch (Exception e) {
            log.error("Failed to send message: ", e);
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        String username = (String) session.getAttributes().get("username");
        log.info("Disconnected To Public: {}", session.getId());
        globalSessions.remove(session);
    }


    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @org.springframework.amqp.rabbit.annotation.Queue(
                    autoDelete = "true",
                    durable = "false"
            ),
            exchange = @org.springframework.amqp.rabbit.annotation.Exchange(
                    value = PUBLIC_EXCHANGE_NAME,
                    type = "topic"
            ),
            key = "global" // Only pull messages meant for the global chat room
    ))
    public void receiveFromRabbitMQ(ChatMessage message) {
        try {
            if (!globalSessions.isEmpty()) {
                String json = objectMapper.writeValueAsString(message);
                TextMessage textMessage = new TextMessage(json);

                // Broadcast the message down to absolutely everyone active in public chat
                for (WebSocketSession session : globalSessions) {
                    if (session.isOpen()) {
                        String sessionUsername = (String) session.getAttributes().get("username");

                        if (sessionUsername != null && sessionUsername.equals(message.getFrom())) {
                            log.debug("Skipping message echo for sender: {}", sessionUsername);
                            continue;
                        }
                        session.sendMessage(textMessage);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast global room update from RabbitMQ", e);
        }
    }
}
