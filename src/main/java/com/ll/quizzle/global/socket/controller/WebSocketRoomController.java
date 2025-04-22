package com.ll.quizzle.global.socket.controller;

import java.util.Objects;

import com.ll.quizzle.domain.room.entity.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.ll.quizzle.domain.room.dto.response.RoomResponse;
import com.ll.quizzle.domain.room.service.RoomService;
import com.ll.quizzle.domain.room.type.RoomStatus;
import com.ll.quizzle.global.socket.core.MessageService;
import com.ll.quizzle.global.socket.core.MessageServiceFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class WebSocketRoomController {
    private final MessageService messageService;
    private final RoomService roomService;

    @Autowired
    public WebSocketRoomController(MessageServiceFactory messageServiceFactory, RoomService roomService) {
        this.messageService = messageServiceFactory.getRoomService();
        this.roomService = roomService;
    }
    
    @MessageMapping("/lobby")
    public void handleLobbyMessage(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("로비 상태 메시지 수신: {}, 사용자: {}", message, username);
        messageService.send("/topic/lobby", message);
    }

    @MessageMapping("/room/{roomId}")
    public void handleRoomMessage(
            @DestinationVariable String roomId,
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("방 상태 메시지 수신: {}, 방: {}, 사용자: {}", message, roomId, username);
        messageService.send("/topic/room/" + roomId, message);
    }

    @MessageMapping("/room/{roomId}/status")
    public void handleRoomStatusMessage(
            @DestinationVariable String roomId,
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("방 상태 업데이트 메시지 수신: {}, 방: {}, 사용자: {}", message, roomId, username);
        
        try {
            Long roomIdLong = Long.parseLong(roomId);
            roomService.broadcastRoomStatus(roomIdLong);
            log.debug("방 ID {} 상태 정보 업데이트 요청 성공", roomId);
        } catch (Exception e) {
            log.error("방 상태 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
            messageService.send("/topic/room/" + roomId + "/status", message);
        }
    }

    @MessageMapping("/room/{roomId}/players/refresh")
    public void handleRefreshPlayers(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("플레이어 목록 갱신 요청: 방 ID {}, 요청자: {}", roomId, username);

        try {
            Long roomIdLong = Long.parseLong(roomId);
            roomService.refreshPlayersList(roomIdLong);
            log.debug("방 ID {} 플레이어 목록 갱신 요청 성공", roomId);
        } catch (Exception e) {
            log.error("플레이어 목록 갱신 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/game/{roomId}")
    public void handleGameMessage(
            @DestinationVariable String roomId,
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("게임 상태 메시지 수신: {}, 방: {}, 사용자: {}", message, roomId, username);
        messageService.send("/topic/game/" + roomId, message);
    }

    @MessageMapping("/game/start/{roomId}")
    public void handleGameStart(
            @DestinationVariable String roomId,
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("게임 시작 메시지 수신: {}, 방: {}, 사용자: {}", message, roomId, username);
        messageService.send("/topic/game/start/" + roomId, message);
    }

    @MessageMapping("/lobby/users")
    public void handleLobbyUsersRequest(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("로비 접속자 목록 요청: {}, 사용자: {}", message, username);
    }

    @MessageMapping("/room/{roomId}/owner/change")
    public void handleOwnerChange(
            @DestinationVariable String roomId,
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("방장 변경 메시지 수신: {}, 방: {}, 사용자: {}", message, roomId, username);
        
        try {
            if (message.contains("newOwnerId") && message.contains("newOwnerNickname")) {
                Long roomIdLong = Long.parseLong(roomId);
                
                String newOwnerIdStr = message.replaceAll(".*\"newOwnerId\"\\s*:\\s*(\\d+).*", "$1");
                Long newOwnerId = Long.parseLong(newOwnerIdStr);
                
                roomService.handleOwnerChange(roomIdLong, newOwnerId);
                
                messageService.send("/topic/room/" + roomId + "/owner/change", message);
                
                log.debug("방 ID {} 방장 변경 완료: 새 방장 ID={}", roomId, newOwnerId);
            } else {
                messageService.send("/topic/room/" + roomId + "/owner/change", message);
            }
        } catch (Exception e) {
            log.error("방장 변경 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/room/{roomId}/status/update")
    public void handleRoomStatusUpdate(
            @DestinationVariable String roomId,
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("방 상태 변경 메시지 수신: {}, 방: {}, 사용자: {}", message, roomId, username);
        
        try {
            Long roomIdLong = Long.parseLong(roomId);
            
            if (message.contains("IN_GAME") || message.contains("GAME_START")) {
                RoomResponse roomResponse = roomService.getRoom(roomIdLong);
                
                if (roomResponse.status() != RoomStatus.IN_GAME) {
                    roomService.updateRoomStatus(roomIdLong, RoomStatus.IN_GAME);
                    log.debug("방 ID {} 상태를 게임 중으로 변경", roomId);
                }
            }
            
            messageService.send("/topic/room/" + roomId + "/status", message);
            
            messageService.send("/topic/lobby", "ROOM_UPDATED:" + roomId);
            
            log.debug("방 ID {} 상태 업데이트 완료", roomId);
        } catch (Exception e) {
            log.error("방 상태 변경 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    @MessageMapping("/lobby/users/update")
    public void handleLobbyUsersUpdate(
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("로비 사용자 상태 업데이트 메시지 수신: {}, 사용자: {}", message, username);
        
        try {
            messageService.send("/topic/lobby/users", message);
            messageService.send("/topic/lobby/status", message);
            
            messageService.send("/topic/lobby/broadcast", message);
            
            log.debug("로비 사용자 목록 업데이트 완료");
        } catch (Exception e) {
            log.error("로비 사용자 상태 업데이트 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/game/{roomId}/player-choice")
    public void handlePlayerChoice(
            @DestinationVariable String roomId,
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("사용자 선택 메시지 수신: {}, 방: {}, 사용자: {}", message, roomId, username);
        
        messageService.send("/topic/game/" + roomId + "/player-choice", message);
    }

    @MessageMapping("/room/{roomId}/scores/update")
    public void handleScoreUpdate(
            @DestinationVariable String roomId,
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("점수 업데이트 메시지 수신: {}, 방: {}, 사용자: {}", message, roomId, username);
        
        messageService.send("/topic/room/" + roomId + "/scores/update", message);
        
        messageService.send("/topic/room/" + roomId + "/scores/broadcast", message);
    }
    
    @MessageMapping("/room/{roomId}/scores/sync")
    public void handleScoreSync(
            @DestinationVariable String roomId,
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("점수 동기화 요청 수신: {}, 방: {}, 사용자: {}", message, roomId, username);
        
        messageService.send("/topic/room/" + roomId + "/scores/sync", message);
        
        messageService.send("/topic/room/" + roomId + "/scores/update", message);
    }
    
    @MessageMapping("/room/{roomId}/timer/start")
    public void handleTimerStart(
            @DestinationVariable String roomId,
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("타이머 시작 메시지 수신: {}, 방: {}, 사용자: {}", message, roomId, username);
        
        messageService.send("/topic/room/" + roomId + "/timer/start", message);
    }
    
    @MessageMapping("/room/{roomId}/timer/expired")
    public void handleTimerExpired(
            @DestinationVariable String roomId,
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("타이머 만료 메시지 수신: {}, 방: {}, 사용자: {}", message, roomId, username);
        
        messageService.send("/topic/room/" + roomId + "/timer/expired", message);
    }

    @MessageMapping("/room/{roomId}/leave")
    public void handlePlayerLeave(
            @DestinationVariable String roomId,
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        log.debug("사용자 방 퇴장 메시지 수신: {}, 방: {}, 사용자: {}", message, roomId, username);

        try {
            Long roomIdLong = Long.parseLong(roomId);

            messageService.send("/topic/room/" + roomId + "/leave", message);

            String playerIdStr = message.replaceAll(".*\"playerId\"\\s*:\\s*(\\d+).*", "$1");
            Long playerId = Long.parseLong(playerIdStr);

            roomService.leaveRoom(roomIdLong, playerId);

            roomService.refreshPlayersList(roomIdLong);

            RoomResponse updatedRoom = roomService.getRoom(roomIdLong);
            int remainingPlayers = updatedRoom.currentPlayers();

            if (remainingPlayers == 0) {
                log.debug("방 ID {} 인원이 0명이 되어 자동 삭제합니다.", roomId);
                
                roomService.deleteRoomById(roomIdLong);
                
                messageService.send("/topic/lobby", "ROOM_DELETED:" + roomId);
            }
        } catch (Exception e) {
            log.error("방 퇴장 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}