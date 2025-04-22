package com.ll.quizzle.global.socket.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.room.entity.Room;
import com.ll.quizzle.domain.room.type.RoomStatus;
import com.ll.quizzle.global.exceptions.ErrorCode;
import com.ll.quizzle.global.socket.core.MessageService;
import com.ll.quizzle.global.socket.core.MessageServiceFactory;
import com.ll.quizzle.global.socket.dto.response.WebSocketRoomMessageResponse;
import com.ll.quizzle.global.socket.type.RoomMessageType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 방 관련 WebSocket 메시지 전송을 처리하는 클래스입니다.
 * 플레이어 목록 정보를 포함한 모든 메시지들은 여기서 중앙 집중적으로 관리를 하도록 설계하였습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketRoomMessageService {

    private final ObjectMapper objectMapper;
    private final MessageServiceFactory messageServiceFactory;
    private final MemberRepository memberRepository;
    private final RedisTemplate<String, Object> redisTemplate;


    public void sendWithPlayersList(Room room, RoomMessageType type, String content, 
                                    String senderId, String senderName) {
        try {
            String playersData = buildPlayersListJson(room);
            sendMessage(room.getId(), type, content, playersData, senderId, senderName);
        } catch (Exception e) {
            log.error("방 메시지 전송 실패: {}", e.getMessage());
            // JSON 파싱이 실패해도 기본 메시지는 전송하게끔 처리
            sendMessage(room.getId(), type, content, null, senderId, senderName);
        }
    }

    public void sendSystemWithPlayersList(Room room, RoomMessageType type, String content) {
        sendWithPlayersList(room, type, content, "SYSTEM", "SYSTEM");
    }

    public void sendGameStart(Room room) {
        sendSystemWithPlayersList(room, RoomMessageType.GAME_START, "게임이 시작되었습니다!");
    }

    public void sendGameEnd(Room room) {
        sendSystemWithPlayersList(room, RoomMessageType.GAME_END, "게임이 종료되었습니다.");
    }

    public void sendReadyStatusChange(Room room, Member member, RoomMessageType type) {

        if (type != RoomMessageType.READY && type != RoomMessageType.UNREADY) {
            ErrorCode.INVALID_READY_MESSAGE_TYPE.throwServiceException();
        }
        
        boolean isReady = (type == RoomMessageType.READY);
        String content = member.getNickname() + "님이 " + (isReady ? "준비" : "준비 해제") + "하셨습니다.";
        
        sendWithPlayersList(room, type, content, member.getId().toString(), member.getNickname());
    }

    public void sendJoin(Room room, Member member) {
        String content = member.getNickname() + "님이 입장하셨습니다.";
        sendWithPlayersList(room, RoomMessageType.JOIN, content, 
                member.getId().toString(), member.getNickname());
    }

    public void sendLeave(Room room, Member member) {
        String content = member.getNickname() + "님이 퇴장하셨습니다.";
        sendWithPlayersList(room, RoomMessageType.LEAVE, content, 
                member.getId().toString(), member.getNickname());
    }

    public void sendOwnerChanged(Room room, Member oldOwner, Member newOwner) {
        String content = oldOwner.getNickname() + "님이 퇴장하여 " + 
                newOwner.getNickname() + "님이 새로운 방장이 되었습니다.";
        sendSystemWithPlayersList(room, RoomMessageType.SYSTEM, content);
    }

    public void sendRoomDeleted(Long roomId) {
        String content = "방장이 퇴장하여 방이 삭제되었습니다.";
        try {
            List<Map<String, Object>> emptyPlayersList = new ArrayList<>();
            String emptyPlayersJson = objectMapper.writeValueAsString(emptyPlayersList);
            
            sendMessage(roomId, RoomMessageType.SYSTEM, content, emptyPlayersJson, "SYSTEM", "SYSTEM");
        } catch (Exception e) {
            log.error("방 삭제 메시지 전송 실패: {}", e.getMessage());
            sendMessage(roomId, RoomMessageType.SYSTEM, content, null, "SYSTEM", "SYSTEM");
        }
    }

    public void sendRoomUpdated(Room room) {
        String content = "방 정보가 업데이트되었습니다.";
        sendSystemWithPlayersList(room, RoomMessageType.ROOM_UPDATED, content);
    }

    private String buildPlayersListJson(Room room) throws JsonProcessingException {
        log.debug("buildPlayersListJson 시작: Room ID={}, 현재 플레이어 수={}", room.getId(), room.getPlayers().size());
        List<Map<String, Object>> playersList = new ArrayList<>();
        
        boolean isGameInProgress = RoomStatus.IN_GAME.equals(room.getStatus());
        String quizId = null;
        String currentRoundKey = null;
        Integer currentRound = null;
        
        if (isGameInProgress) {
            quizId = room.getId().toString();
            currentRoundKey = String.format("quiz:%s:currentRound", quizId);
            Object roundObj = redisTemplate.opsForValue().get(currentRoundKey);
            currentRound = roundObj != null ? Integer.valueOf(roundObj.toString()) : null;
            
            log.debug("진행 중인 게임 확인: 룸={}, 퀴즈ID={}, 현재라운드={}", room.getId(), quizId, currentRound);
        }
        
        // 플레이어 ID 목록을 로깅
        log.debug("플레이어 ID 목록: {}", room.getPlayers());
        
        if (room.getPlayers().isEmpty()) {
            log.warn("Room ID={} 플레이어 목록이 비어 있습니다.", room.getId());
            return "[]";
        }
        
        for (Long playerId : room.getPlayers()) {
            log.debug("플레이어 처리: ID={}", playerId);
            
            try {
                Member playerMember = memberRepository.findById(playerId).orElse(null);
                if (playerMember != null) {
                    Map<String, Object> playerInfo = new HashMap<>();
                    playerInfo.put("id", playerMember.getId().toString());
                    playerInfo.put("name", playerMember.getNickname());
                    playerInfo.put("isReady", room.getReadyPlayers().contains(playerId));
                    playerInfo.put("isOwner", room.isOwner(playerId));
                    
                    // 추가 정보 - 아바타 URL 등
                    playerInfo.put("nickname", playerMember.getNickname()); // 추가 속성
                    
                    if (isGameInProgress && currentRound != null) {
                        String userId = playerMember.getId().toString();
                        String submissionKey = String.format("quiz:%s:user:%s:submissions", quizId, userId);
                        Long submissionsCount = redisTemplate.opsForList().size(submissionKey);
                        
                        boolean hasSubmitted = submissionsCount != null && submissionsCount >= currentRound;
                        playerInfo.put("isSubmitted", hasSubmitted);
                        
                        log.debug("플레이어 제출 여부: 사용자={}, 현재라운드={}, 제출여부={}", userId, currentRound, hasSubmitted);
                    } else {
                        playerInfo.put("isSubmitted", false);
                    }
                    playersList.add(playerInfo);
                    
                    log.debug("플레이어 정보 추가: ID={}, 닉네임={}", playerId, playerMember.getNickname());
                } else {
                    log.warn("해당 ID의 멤버를 찾을 수 없습니다: {}", playerId);
                }
            } catch (Exception e) {
                log.error("플레이어 정보 처리 중 오류 발생: 플레이어ID={}, 오류={}", playerId, e.getMessage(), e);
            }
        }
        
        String jsonResult = objectMapper.writeValueAsString(playersList);
        log.debug("buildPlayersListJson 완료: Room ID={}, 생성된 JSON={}, 플레이어 수={}", 
                room.getId(), jsonResult, playersList.size());
        return jsonResult;
    }


    private void sendMessage(Long roomId, RoomMessageType type, String content, 
                           String data, String senderId, String senderName) {
        MessageService roomService = messageServiceFactory.getRoomService();
        WebSocketRoomMessageResponse message = WebSocketRoomMessageResponse.of(
            type,
            content,
            data,
            senderId,
            senderName,
            System.currentTimeMillis(),
            roomId.toString()
        );
        
        roomService.send("/topic/room/" + roomId, message);
    }
} 