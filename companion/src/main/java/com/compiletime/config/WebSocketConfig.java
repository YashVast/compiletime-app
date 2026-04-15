package com.compiletime.config;

import com.compiletime.infrastructure.ExtensionSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ExtensionSocketHandler extensionSocketHandler;

    public WebSocketConfig(ExtensionSocketHandler extensionSocketHandler) {
        this.extensionSocketHandler = extensionSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            .addHandler(extensionSocketHandler, "/ws")
            // Chrome extensions don't send an Origin header that matches localhost,
            // so we allow all origins for the local WebSocket endpoint.
            .setAllowedOrigins("*");
    }
}
