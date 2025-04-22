package com.ll.quizzle.global.socket.controller;

import java.security.Principal;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.ll.quizzle.global.security.oauth2.dto.SecurityUser;
import com.ll.quizzle.global.socket.core.MessageService;
import com.ll.quizzle.global.socket.core.MessageServiceFactory;
import com.ll.quizzle.global.socket.dto.response.WebSocketChatMessageResponse;
import com.ll.quizzle.global.socket.type.MessageType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class WebSocketChatController {
    private final MessageService chatService;

    @Autowired
    public WebSocketChatController(MessageServiceFactory messageServiceFactory) {
        this.chatService = messageServiceFactory.getChatService();
    }
    
    @MessageMapping("/lobby/chat")
    public void handleLobbyChatMessage(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        String username = Objects.requireNonNull(principal).getName();
        
        Long userId = null;
        if (principal instanceof Authentication) {
            Object userObj = ((Authentication) principal).getPrincipal();
            if (userObj instanceof SecurityUser) {
                userId = ((SecurityUser) userObj).getId();
            }
        }
        MessageType messageType = MessageType.CHAT;
        String messageContent = message;
        
        if (message != null && message.startsWith("!SYSTEM ")) {
            messageType = MessageType.SYSTEM;
            messageContent = message.substring(8);
            log.debug("시스템 메시지 감지: {}", messageContent);
        }
        
        log.debug("로비 채팅 메시지 수신: {}, 사용자: {}, ID: {}, 타입: {}", messageContent, username, userId, messageType);
        
        WebSocketChatMessageResponse response = WebSocketChatMessageResponse.of(
            messageType,
            messageContent,
            userId,
            username,
            System.currentTimeMillis(),
            "lobby"
        );
        
        chatService.send("/topic/lobby/chat", response);
    }

    @MessageMapping("/room/chat/{roomId}")
    public void handleRoomChatMessage(
            @DestinationVariable String roomId,
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Principal principal = headerAccessor.getUser();
        String username = Objects.requireNonNull(principal).getName();
        
        Long userId = null;
        if (principal instanceof Authentication) {
            Object userObj = ((Authentication) principal).getPrincipal();
            if (userObj instanceof SecurityUser) {
                userId = ((SecurityUser) userObj).getId();
            }
        }
        
        MessageType messageType = MessageType.CHAT;
        String messageContent = message;
        
        if (message != null && message.startsWith("!SYSTEM ")) {
            messageType = MessageType.SYSTEM;
            messageContent = message.substring(8);
            log.debug("시스템 메시지 감지: {}, 방: {}", messageContent, roomId);
        }
        
        log.debug("방 채팅 메시지 수신: {}, 방: {}, 사용자: {}, ID: {}, 타입: {}", messageContent, roomId, username, userId, messageType);
        
        WebSocketChatMessageResponse response = WebSocketChatMessageResponse.of(
            messageType,
            messageContent,
            userId,
            username,
            System.currentTimeMillis(),
            roomId
        );
        
        chatService.send("/topic/room/chat/" + roomId, response);
    }

    @MessageMapping("/game/chat/{roomId}")
    public void handleGameChatMessage(
            @DestinationVariable String roomId,
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Principal principal = headerAccessor.getUser();
        String username = Objects.requireNonNull(principal).getName();
        
        Long userId = null;
        if (principal instanceof Authentication) {
            Object userObj = ((Authentication) principal).getPrincipal();
            if (userObj instanceof SecurityUser) {
                userId = ((SecurityUser) userObj).getId();
            }
        }
        
        MessageType messageType = MessageType.CHAT;
        String messageContent = message;
        
        if (message != null && message.startsWith("!SYSTEM ")) {
            messageType = MessageType.SYSTEM;
            messageContent = message.substring(8);
            log.debug("시스템 메시지 감지: {}, 게임방: {}", messageContent, roomId);
        }
        
        log.debug("게임 채팅 메시지 수신: {}, 방: {}, 사용자: {}, ID: {}, 타입: {}", messageContent, roomId, username, userId, messageType);
        
        WebSocketChatMessageResponse response = WebSocketChatMessageResponse.of(
            messageType,
            messageContent,
            userId,
            username,
            System.currentTimeMillis(),
            roomId
        );
        
        chatService.send("/topic/game/chat/" + roomId, response);
    }
} 