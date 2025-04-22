package com.ll.quizzle.global.socket.config;

import com.ll.quizzle.global.socket.core.MessageServiceConstants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "quizzle.messaging")
public class MessageServiceProperties {
    private final WebSocketRoom websocketRoom;
    private final WebSocketChat websocketChat;

    @Getter
    @Setter
    public static class WebSocketRoom {
        private String provider = MessageServiceConstants.PROVIDER_STOMP;
    }

    @Getter
    @Setter
    public static class WebSocketChat {
        private String provider = MessageServiceConstants.PROVIDER_REDIS;
    }
} 