package com.kid.A0.websocket.rabbitMQ;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String topicExchangeName = "chat.exchange";
    private static final String queueName = "queue";
    private static final String routingKey = "chat.publish.#";
//
//    @Bean
//    Queue queue() {
//        return new Queue(queueName, true);
//    }

    @Bean
    TopicExchange topicExchange() {
        return new TopicExchange(topicExchangeName);
    }
    @Bean
    public JacksonJsonMessageConverter messageConverter(){
        return new JacksonJsonMessageConverter();
    }
//
//    @Bean
//    MessageListenerAdapter listenerAdapter(Receiver receiver) {
//        return new MessageListenerAdapter(receiver, "receive");
//    }
//
//    @Bean
//    Binding binding(Queue queue, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queue).to(topicExchange).with(routingKey);
//    }
//
//
//    @Bean
//    SimpleMessageListenerContainer simpleMessageListenerContainer(ConnectionFactory factory,MessageListenerAdapter listenerAdapter){
//        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
//        container.setMessageListener(listenerAdapter);
//        container.setConnectionFactory(factory);
//        container.setQueueNames(queueName);
//        return container;
//    }
}
