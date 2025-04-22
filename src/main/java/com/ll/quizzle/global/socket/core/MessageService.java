package com.ll.quizzle.global.socket.core;

/**
 * 메시징 서비스를 위한 추상 인터페이스 (WebSocket, STOMP, Redis, RabbitMQ, Kafka 등 확장성을 고려)
 */
public interface MessageService {

    void send(String destination, Object message);

    void sendToUser(String userId, Object message);

    String subscribe(String destination, MessageCallback callback);

    void unsubscribe(String subscriptionId);

    boolean isConnected();

    void connect();

    void disconnect();
} 