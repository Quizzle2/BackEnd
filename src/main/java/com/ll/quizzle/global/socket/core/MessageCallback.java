package com.ll.quizzle.global.socket.core;

/**
 * 메시지 수신 시 호출될 콜백 인터페이스
 */
@FunctionalInterface
public interface MessageCallback {

    /**
     * 수신된 메시지를 처리하는 콜백 메서드, 시스템에서 메시지를 수신할 때 호출되는 메시지를 처리할 때 이 메서드를 이용해서 처리하시면 됩니다.
     */
    void onMessage(Object message);
} 