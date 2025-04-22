package com.ll.quizzle.domain.room.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.room.entity.Room;
import com.ll.quizzle.domain.room.repository.RoomRepository;
import com.ll.quizzle.domain.room.type.AnswerType;
import com.ll.quizzle.domain.room.type.Difficulty;
import com.ll.quizzle.domain.room.type.MainCategory;
import com.ll.quizzle.domain.room.type.RoomStatus;
import com.ll.quizzle.domain.room.type.SubCategory;
import com.ll.quizzle.global.exceptions.ServiceException;
import com.ll.quizzle.global.redis.lock.DistributedLockService;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class RoomServiceGameStartConcurrencyTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private DistributedLockService redisLockService;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private Avatar defaultAvatar;

    @InjectMocks
    private RoomService roomService;

    private MockedStatic<TransactionSynchronizationManager> mockedTransactionManager;
    private Room testRoom;
    private Member testOwner;

    @BeforeEach
    void setUp() {
        mockedTransactionManager = mockStatic(TransactionSynchronizationManager.class);
        mockedTransactionManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                .thenAnswer(invocation -> null);

        testOwner = Member.create("방장", "owner@example.com", defaultAvatar);
        ReflectionTestUtils.setField(testOwner, "id", 1L);

        testRoom = Room.builder()
                .title("테스트 방")
                .owner(testOwner)
                .capacity(4)
                .difficulty(Difficulty.NORMAL)
                .mainCategory(MainCategory.GENERAL_KNOWLEDGE)
                .subCategory(SubCategory.CULTURE)
                .answerType(AnswerType.MULTIPLE_CHOICE)
                .build();

        ReflectionTestUtils.setField(testRoom, "id", 1L);
        ReflectionTestUtils.setField(testRoom, "status", RoomStatus.WAITING);

        Set<Long> players = new HashSet<>();
        players.add(1L);
        players.add(2L);

        Set<Long> readyPlayers = new HashSet<>();
        readyPlayers.add(2L);

        ReflectionTestUtils.setField(testRoom, "players", players);
        ReflectionTestUtils.setField(testRoom, "readyPlayers", readyPlayers);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisLockService.acquireLock(anyString(), anyLong(), anyLong())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        if (mockedTransactionManager != null) {
            mockedTransactionManager.close();
        }
    }

    @Test
    @DisplayName("게임 시작 동시성 테스트 - 두 번째 시작 요청은 예외 발생")
    void startGameConcurrencyTest() {
        // given
        Room spyRoom = spy(testRoom);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(spyRoom));

        when(spyRoom.isOwner(1L)).thenReturn(true);
        when(spyRoom.isAllPlayersReady()).thenReturn(true);

        Set<Long> players = new HashSet<>();
        players.add(1L);
        players.add(2L);
        when(spyRoom.getPlayers()).thenReturn(players);

        when(valueOperations.get("room:game-state:1"))
                .thenReturn(null)
                .thenReturn("IN_GAME");

        // when & then
        roomService.startGame(1L, 1L);

        assertThatThrownBy(() -> roomService.startGame(1L, 1L))
                .isInstanceOf(ServiceException.class);

        verify(spyRoom, times(1)).startGame(1L);
        verify(valueOperations, times(1)).set(eq("room:game-state:1"), eq("IN_GAME"));
    }

    @Test
    @DisplayName("게임 시작 중 플레이어 이탈 테스트")
    void startGamePlayerLeaveTest() {
        // given
        Room spyRoom = spy(testRoom);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(spyRoom));

        when(spyRoom.isOwner(1L)).thenReturn(true);
        when(spyRoom.isAllPlayersReady()).thenReturn(true);

        Set<Long> initialPlayers = new HashSet<>();
        initialPlayers.add(1L);
        initialPlayers.add(2L);

        Set<Long> reducedPlayers = new HashSet<>();
        reducedPlayers.add(1L);

        when(spyRoom.getPlayers())
                .thenReturn(initialPlayers)
                .thenReturn(reducedPlayers);

        when(valueOperations.get(anyString())).thenReturn(null);

        doAnswer(invocation -> {
            when(spyRoom.getPlayers()).thenReturn(reducedPlayers);
            return null;
        }).when(spyRoom).startGame(anyLong());

        // when
        roomService.startGame(1L, 1L);

        // then
        verify(spyRoom, times(1)).startGame(1L);
        verify(valueOperations, times(1)).set(eq("room:game-state:1"), eq("IN_GAME"));
        verify(spyRoom, atLeast(2)).getPlayers();
    }
}