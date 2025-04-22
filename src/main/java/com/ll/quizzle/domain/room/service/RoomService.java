package com.ll.quizzle.domain.room.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.quiz.dto.request.QuizGenerationRequest;
import com.ll.quizzle.domain.quiz.dto.response.QuizResponse;
import com.ll.quizzle.domain.quiz.service.GPTQuizService;
import com.ll.quizzle.domain.quiz.service.QuizParticipantService;
import com.ll.quizzle.domain.room.dto.request.RoomCreateRequest;
import com.ll.quizzle.domain.room.dto.request.RoomUpdateRequest;
import com.ll.quizzle.domain.room.dto.response.RoomResponse;
import com.ll.quizzle.domain.room.entity.Room;
import com.ll.quizzle.domain.room.repository.RoomRepository;
import com.ll.quizzle.domain.room.type.RoomStatus;
import static com.ll.quizzle.global.exceptions.ErrorCode.GAME_ALREADY_STARTED;
import static com.ll.quizzle.global.exceptions.ErrorCode.INVALID_PASSWORD;
import static com.ll.quizzle.global.exceptions.ErrorCode.MEMBER_NOT_FOUND;
import static com.ll.quizzle.global.exceptions.ErrorCode.MIN_PLAYER_COUNT_NOT_MET;
import static com.ll.quizzle.global.exceptions.ErrorCode.NOT_ALL_PLAYERS_READY;
import static com.ll.quizzle.global.exceptions.ErrorCode.NOT_ROOM_OWNER;
import static com.ll.quizzle.global.exceptions.ErrorCode.ROOM_ENTRY_RESTRICTED;
import static com.ll.quizzle.global.exceptions.ErrorCode.ROOM_IS_FULL;
import static com.ll.quizzle.global.exceptions.ErrorCode.ROOM_NOT_FOUND;
import com.ll.quizzle.global.redis.lock.DistributedLock;
import com.ll.quizzle.global.redis.lock.DistributedLockService;
import com.ll.quizzle.global.socket.core.MessageService;
import com.ll.quizzle.global.socket.core.MessageServiceFactory;
import com.ll.quizzle.global.socket.service.WebSocketRoomMessageService;
import com.ll.quizzle.global.socket.type.RoomMessageType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {
    private final RoomRepository roomRepository;

    private final MemberRepository memberRepository;
    private final RoomBlacklistService blacklistService;
    private final DistributedLockService redisLockService;
    private final RedisTemplate<String, String> redisTemplate;
    private final WebSocketRoomMessageService roomMessageService;
    private final MessageServiceFactory messageServiceFactory;
    private final GPTQuizService  gptQuizService;
    private final  QuizParticipantService quizParticipantService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public RoomResponse createRoom(Long ownerId, RoomCreateRequest request) {
        Member owner = findMemberOrThrow(ownerId);
        return createRoomWithLock(owner, request);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    public RoomResponse updateRoom(Long roomId, Long ownerId, RoomUpdateRequest request) {
        Room room = validateRoomForUpdate(roomId, ownerId);

        return updateRoomWithLock(room, request);
    }

    public List<RoomResponse> getActiveRooms() {
        List<Room> rooms = roomRepository.findByStatusNot(RoomStatus.FINISHED);
        return rooms.stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void joinRoom(Long roomId, Long memberId, String password) {
        Room room = findRoomOrThrow(roomId);
        Member member = findMemberOrThrow(memberId);

        validate(roomId, memberId, password, room);
        joinRoomWithLock(room, member);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void leaveRoom(Long roomId, Long memberId) {
        Room room = findRoomOrThrow(roomId);
        Member member = findMemberOrThrow(memberId);

        leaveRoomWithLock(room, member);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void handleDisconnect(Long memberId) {
        List<Room> rooms = roomRepository.findRoomsByPlayerId(memberId);

        if (rooms.isEmpty()) {
            return;
        }

        for (Room room : rooms) {
            try {
                String lockKey = "lock:room:" + room.getId();
                boolean locked = redisLockService.acquireLock(lockKey, 5000, 10000);

                if (locked) {
                    try {
                        Room lockedRoom = roomRepository.findById(room.getId())
                                .orElseThrow(ROOM_NOT_FOUND::throwServiceException);

                        if (lockedRoom.hasPlayer(memberId)) {
                            leaveRoomInternal(lockedRoom, memberId);
                        }
                    } finally {
                        redisLockService.releaseLock(lockKey);
                    }
                } else {
                    log.debug("연결 해제 처리 중 락 획득 실패: 방ID={}, 멤버ID={}", room.getId(), memberId);
                }
            } catch (Exception e) {
                log.error("연결 해제 처리 중 오류 발생 - 방ID: {}, 멤버ID: {}, 오류: {}",
                        room.getId(), memberId, e.getMessage());
            }
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void toggleReady(Long roomId, Long memberId) {
        Room room = findRoomOrThrow(roomId);

        if (room.isOwner(memberId)) {
            return;
        }

        boolean isCurrentlyReady = room.getReadyPlayers().contains(memberId);

        setReadyWithLock(room, memberId, !isCurrentlyReady);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void startGame(Long roomId, Long memberId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(ROOM_NOT_FOUND::throwServiceException);

        startGameWithLock(room, memberId);
    }

    public RoomResponse getRoom(Long roomId) {
        Room room = findRoomOrThrow(roomId);
        return RoomResponse.from(room);
    }

    @DistributedLock(key = "'room:' + #roomId", leaseTime = 10000)
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void updateRoomStatus(Long roomId, RoomStatus status) {
        Room room = findRoomOrThrow(roomId);
        
        if (room.getStatus() == status) {
            log.debug("방 ID {} 상태 변경 무시 - 이미 {}상태임", roomId, status);
            return;
        }
        
        if (status == RoomStatus.IN_GAME) {
            room.startGame(null);
        } else if (status == RoomStatus.WAITING) {
            room.endGame();
        }
        
        log.debug("방 ID {} 상태 변경 완료: {} -> {}", roomId, room.getStatus(), status);
        
        broadcastRoomStatus(roomId);
    }

    @DistributedLock(key = "'room:' + #roomId", leaseTime = 10000)
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void handleOwnerChange(Long roomId, Long newOwnerId) {
        Room room = findRoomOrThrow(roomId);
        
        if (!room.getPlayers().contains(newOwnerId)) {
            log.error("방장 변경 실패 - 존재하지 않는 플레이어: roomId={}, newOwnerId={}", roomId, newOwnerId);
            throw MEMBER_NOT_FOUND.throwServiceException();
        }
        
        if (room.isOwner(newOwnerId)) {
            log.debug("방장 변경 중단 - 이미 방장인 플레이어: roomId={}, ownerId={}", roomId, newOwnerId);
            return;
        }
        
        Member oldOwner = findMemberOrThrow(room.getOwner().getId());
        
        Member newOwner = findMemberOrThrow(newOwnerId);
        room.changeOwner(newOwner);
        
        roomMessageService.sendOwnerChanged(room, oldOwner, newOwner);
        
        log.debug("방장 변경 완료: roomId={}, 이전 방장={}, 새 방장={}", 
                  roomId, oldOwner.getNickname(), newOwner.getNickname());
    }

    @DistributedLock(key = "'room:' + #owner.id", leaseTime = 10000)
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    protected RoomResponse createRoomWithLock(Member owner, RoomCreateRequest request) {
        Room room = Room.builder()
                .title(request.title())
                .owner(owner)
                .capacity(request.capacity())
                .difficulty(request.difficulty())
                .mainCategory(request.mainCategory())
                .subCategory(request.subCategory())
                .answerType(request.answerType())
                .problemCount(request.problemCount())
                .password(request.isPrivate() ? request.password() : null)
                .build();

        Room savedRoom = roomRepository.save(room);

        scheduleRoomCreatedNotification(savedRoom);

        return RoomResponse.from(savedRoom);
    }

    private void validate(Long roomId, Long memberId, String password, Room room) {
        if (blacklistService.isBlacklisted(roomId, memberId)) {
            throw ROOM_ENTRY_RESTRICTED.throwServiceException();
        }

        if (!room.validatePassword(password)) {
            throw INVALID_PASSWORD.throwServiceException();
        }

        if (room.isFull()) {
            ROOM_IS_FULL.throwServiceException();
        }
    }

    @DistributedLock(key = "'room:' + #room.id", leaseTime = 10000)
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    protected void joinRoomWithLock(Room room, Member member) {
        if (!room.hasPlayer(member.getId())) {
            Long roomId = room.getId();
            Long memberId = member.getId();

            room.addPlayer(memberId);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        Room freshRoom = findRoomOrThrow(roomId);
                        Member freshMember = findMemberOrThrow(memberId);
                        
                        log.debug("afterCommit: Room ID {} 최신 플레이어 수: {}, 플레이어 목록: {}", 
                                roomId, freshRoom.getPlayers().size(), freshRoom.getPlayers());
                                
                        roomMessageService.sendJoin(freshRoom, freshMember);
                        
                        roomMessageService.sendRoomUpdated(freshRoom);
                        
                        MessageService roomService = messageServiceFactory.getRoomService();
                        roomService.send("/topic/lobby", "ROOM_UPDATED:" + roomId);
                    } catch (Exception e) {
                        log.error("afterCommit 중 오류 발생 (sendJoin): Room ID={}, Member ID={}, Error: {}", 
                                roomId, memberId, e.getMessage(), e);
                    }
                }
            });
        }
    }

    private void handleGameEnd(Room room, String roomGameStateKey) {
        room.endGame();
        redisTemplate.opsForValue().set(roomGameStateKey, "ENDED");
    }

    public void broadcastRoomStatus(Long roomId) {
        Room room = findRoomOrThrow(roomId);
        roomMessageService.sendRoomUpdated(room);
    }

    public void refreshPlayersList(Long roomId) {
        Room room = findRoomOrThrow(roomId);
        roomMessageService.sendRoomUpdated(room);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteRoom(Room room) {
        String roomStateKey = "room:state:" + room.getId();
        redisTemplate.opsForValue().set(roomStateKey, "DELETING");

        roomRepository.delete(room);

        redisTemplate.opsForValue().set(roomStateKey, "DELETED");

        roomMessageService.sendRoomDeleted(room.getId());

        scheduleRoomDeletedNotification(room.getId());
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    protected void changeRoomOwner(Room room, Member currentOwner, Long newOwnerId) {
        Member newOwner = findMemberOrThrow(newOwnerId);
        room.changeOwner(newOwner);

        roomMessageService.sendOwnerChanged(room, currentOwner, newOwner);
    }

    @DistributedLock(key = "'room:' + #room.id", leaseTime = 10000)
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    protected void leaveRoomWithLock(Room room, Member member) {
        boolean isOwner = room.isOwner(member.getId());

        room.removePlayer(member.getId());

        String roomGameStateKey = "room:game-state:" + room.getId();
        String gameState = redisTemplate.opsForValue().get(roomGameStateKey);

        boolean shouldEndGame = "IN_GAME".equals(gameState) && room.getPlayers().isEmpty();

        if (shouldEndGame) {
            handleGameEnd(room, roomGameStateKey);
        }

        if (room.getPlayers().isEmpty()) {
            log.debug("방에 더 이상 플레이어가 없어 방을 즉시 삭제합니다. 방ID: {}", room.getId());
            roomRepository.delete(room);
            String roomStateKey = "room:state:" + room.getId();
            redisTemplate.opsForValue().set(roomStateKey, "DELETED");

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    roomMessageService.sendRoomDeleted(room.getId());
                    scheduleRoomDeletedNotification(room.getId());
                }
            });
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (shouldEndGame) {
                    roomMessageService.sendGameEnd(room);
                }

                if (isOwner) {
                    if (room.getPlayers().isEmpty()) {
                    } else {
                        Long newOwnerId = room.getPlayers().iterator().next();
                        changeRoomOwner(room, member, newOwnerId);
                    }
                } else {
                    roomMessageService.sendLeave(room, member);
                }
            }
        });
    }

    protected void leaveRoomInternal(Room room, Long memberId) {
        Member member = findMemberOrThrow(memberId);

        boolean isOwner = room.isOwner(memberId);

        room.removePlayer(memberId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (room.getPlayers().isEmpty()) {
                    deleteRoom(room);
                } else if (isOwner) {
                    Long newOwnerId = room.getPlayers().iterator().next();
                    Member newOwner = findMemberOrThrow(newOwnerId);

                    room.changeOwner(newOwner);

                    roomMessageService.sendOwnerChanged(room, member, newOwner);
                } else {
                    roomMessageService.sendLeave(room, member);
                }
            }
        });
    }

    @DistributedLock(key = "'room:' + #room.id", leaseTime = 10000)
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    protected void setReadyWithLock(Room room, Long memberId, boolean isReady) {
        Member member = findMemberOrThrow(memberId);

        if (isReady) {
            room.playerReady(memberId);
        } else {
            room.playerUnready(memberId);
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                roomMessageService.sendReadyStatusChange(
                        room,
                        member,
                        isReady ? RoomMessageType.READY : RoomMessageType.UNREADY);
            }
        });
    }

    @DistributedLock(key = "'room:' + #room.id", leaseTime = 10000)
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    protected void startGameWithLock(Room room, Long memberId) {
        int initialPlayerCount = room.getPlayers().size();
        log.debug("게임 시작 요청 - 방ID: {}, 방장ID: {}, 초기 플레이어 수: {}", room.getId(), memberId, initialPlayerCount);


        validateGameStart(room, memberId);
        String roomStateKey = validateGameState(room);

        try {
            processGameStart(room, memberId, initialPlayerCount, roomStateKey);
            QuizGenerationRequest quizRequest = new QuizGenerationRequest(
                    room.getMainCategory(),
                    room.getSubCategory(),
                    room.getAnswerType(),
                    room.getProblemCount(),
                    room.getDifficulty(),
                    room.getId().toString()
            );

            QuizResponse quizResponse = gptQuizService.generateQuiz(quizRequest);
            String quizId = quizResponse.quizId();

            for (Long playerId : room.getPlayers()) {
                quizParticipantService.registerParticipant(quizId, playerId);
            }

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    roomMessageService.sendGameStart(room);
                }
            });

        } catch (Exception e) {
            redisTemplate.opsForValue().set(roomStateKey, "WAITING");
            log.error("게임 시작 중 오류 발생 - 방ID: {}, 오류: {}", room.getId(), e.getMessage());
            throw e;
        }
    }


    private void processGameStart(Room room, Long memberId, int initialPlayerCount, String roomStateKey) {
        room.startGame(memberId);
        log.debug("게임 시작 처리 완료 - 방ID: {}", room.getId());

        if (room.getPlayers().size() < initialPlayerCount) {
            log.debug("게임 시작 중 플레이어 수 변경 감지 - 방ID: {}, 초기: {}, 현재: {}",
                    room.getId(), initialPlayerCount, room.getPlayers().size());

            if (room.getPlayers().isEmpty()) {
                log.error("게임 시작 실패 - 최종 플레이어 수 미달 - 방ID: {}, 현재 플레이어 수: {}",
                        room.getId(), 0);
                throw MIN_PLAYER_COUNT_NOT_MET.throwServiceException();
            }
        }

        redisTemplate.opsForValue().set(roomStateKey, "IN_GAME");
        log.debug("게임 시작 상태 저장 완료 (Redis) - 방ID: {}, 상태: IN_GAME", room.getId());
    }

    private String validateGameState(Room room) {
        if (RoomStatus.IN_GAME.equals(room.getStatus())) {
            log.debug("게임 시작 실패 - 이미 게임이 시작된 상태 (DB) - 방ID: {}", room.getId());
            throw GAME_ALREADY_STARTED.throwServiceException();
        }

        String roomStateKey = "room:game-state:" + room.getId();
        String currentState = redisTemplate.opsForValue().get(roomStateKey);
        log.debug("방 상태 확인 (Redis) - 방ID: {}, 상태: {}", room.getId(), currentState);

        if ("IN_GAME".equals(currentState)) {
            log.debug("게임 시작 실패 - 이미 게임이 시작된 상태 (Redis) - 방ID: {}", room.getId());
            throw GAME_ALREADY_STARTED.throwServiceException();
        }

        redisTemplate.opsForValue().set(roomStateKey, "STARTING");
        log.debug("게임 시작 임시 상태 설정 - 방ID: {}, 상태: STARTING", room.getId());

        return roomStateKey;
    }

    protected Room findRoomOrThrow(Long roomId) {
        return roomRepository.findRoomById(roomId)
                .orElseThrow(ROOM_NOT_FOUND::throwServiceException);
    }

    protected Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MEMBER_NOT_FOUND::throwServiceException);
    }

    private void validateGameStart(Room room, Long memberId) {
        if (!room.isOwner(memberId)) {
            log.error("게임 시작 중 오류 발생 - 방ID: {}, 오류: 403 FORBIDDEN : 방장만 이 작업을 수행할 수 있습니다.", room.getId());
            throw NOT_ROOM_OWNER.throwServiceException();
        }

        if (!room.isAllPlayersReady()) {
            log.error("게임 시작 중 오류 발생 - 방ID: {}, 오류: 모든 플레이어가 준비되지 않았습니다.", room.getId());
            throw NOT_ALL_PLAYERS_READY.throwServiceException();
        }
    }

    private Room validateRoomForUpdate(Long roomId, Long ownerId) {
        Room room = findRoomOrThrow(roomId);

        if (!room.isOwner(ownerId)) {
            throw NOT_ROOM_OWNER.throwServiceException();
        }

        if (RoomStatus.IN_GAME.equals(room.getStatus())) {
            throw GAME_ALREADY_STARTED.throwServiceException();
        }

        return room;
    }

    @DistributedLock(key = "'room:' + #room.id", leaseTime = 10000)
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    protected RoomResponse updateRoomWithLock(Room room, RoomUpdateRequest request) {
        updateRoomProperties(room, request);

        Room updatedRoom = roomRepository.save(room);
        scheduleNotifications(updatedRoom);

        return RoomResponse.from(updatedRoom);
    }


    private void updateRoomProperties(Room room, RoomUpdateRequest request) {
        String password = request.isPrivate() ? request.password() : null;
        Boolean isPrivate = request.isPrivate();

        room.updateRoom(
            request.title(),
            request.capacity() > 0 ? request.capacity() : null,
            request.difficulty(),
            request.mainCategory(),
            request.subCategory(),
            password,
            isPrivate
        );
    }

    private void scheduleRoomCreatedNotification(Room savedRoom) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                MessageService roomService = messageServiceFactory.getRoomService();
                roomService.send("/topic/lobby", "ROOM_CREATED:" + savedRoom.getId());
                log.debug("로비에 방 생성 알림 전송: 방ID={}", savedRoom.getId());
            }
        });
    }

    private void scheduleRoomDeletedNotification(Long roomId) {
        MessageService roomService = messageServiceFactory.getRoomService();
        roomService.send("/topic/lobby", "ROOM_DELETED:" + roomId);
        log.debug("로비에 방 삭제 알림 전송: 방ID={}", roomId);
    }

    private void scheduleNotifications(Room updatedRoom) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                roomMessageService.sendRoomUpdated(updatedRoom);

                MessageService roomService = messageServiceFactory.getRoomService();
                roomService.send("/topic/lobby", "ROOM_UPDATED:" + updatedRoom.getId());
                log.debug("로비에 방 업데이트 알림 전송: 방ID={}", updatedRoom.getId());
            }
        });
    }

    @DistributedLock(key = "'room:' + #roomId", leaseTime = 10000)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteRoomById(Long roomId) {
        Room room = findRoomOrThrow(roomId);
        deleteRoom(room);
    }
}