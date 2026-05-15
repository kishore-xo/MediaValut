package com.kid.A0.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private static final String PUBLIC_CHAT = "chat.public.exchange";
    private static final String PRIVATE_CHAT = "chat.private.exchange";

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(PRIVATE_CHAT);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(PUBLIC_CHAT);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
