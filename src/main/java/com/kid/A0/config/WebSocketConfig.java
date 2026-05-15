package com.kid.A0.config;

import com.kid.A0.websocket.MyHandShakeInterceptor;
import com.kid.A0.websocket.PrivateHandler;
import com.kid.A0.websocket.PublicHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer  {

    private final PublicHandler publicHandler;
    private final PrivateHandler privateHandler;
    private final MyHandShakeInterceptor myHandShakeInterceptor;

    public WebSocketConfig(PublicHandler publicHandler, PrivateHandler privateHandler, MyHandShakeInterceptor myHandShakeInterceptor) {
        this.publicHandler = publicHandler;
        this.privateHandler = privateHandler;
        this.myHandShakeInterceptor = myHandShakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(publicHandler, "/ws/public")
                .addInterceptors(myHandShakeInterceptor)
                .setAllowedOrigins("*");

        registry.addHandler(privateHandler, "/ws/private")
                .addInterceptors(myHandShakeInterceptor)
                .setAllowedOrigins("*");
    }

}
