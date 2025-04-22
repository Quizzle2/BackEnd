package com.ll.quizzle.global.socket.core;

import java.io.Serial;
import java.io.Serializable;

public record SessionInfo(String accessToken, long expiryTime, String sessionId) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
} 