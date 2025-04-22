package com.ll.quizzle.domain.room.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
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
class RoomServiceConcurrencyTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private RoomBlacklistService blacklistService;
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
    private List<Member> testMembers;

    @BeforeEach
    void setUp() {
        mockedTransactionManager = mockStatic(TransactionSynchronizationManager.class);
        mockedTransactionManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                .thenAnswer(invocation -> null);

        testOwner = Member.create("방장", "owner@example.com", defaultAvatar);
        ReflectionTestUtils.setField(testOwner, "id", 1L);

        testRoom = Room.builder()
                .title("테스트 방")
                .owner(testOwner)
                .capacity(2)
                .difficulty(Difficulty.NORMAL)
                .mainCategory(MainCategory.GENERAL_KNOWLEDGE)
                .subCategory(SubCategory.CULTURE)
                .answerType(AnswerType.MULTIPLE_CHOICE)
                .build();

        ReflectionTestUtils.setField(testRoom, "id", 1L);
        ReflectionTestUtils.setField(testRoom, "status", RoomStatus.WAITING);

        testMembers = new ArrayList<>();
        for (int i = 2; i <= 4; i++) {
            Member member = Member.create("유저" + i, "user" + i + "@example.com", defaultAvatar);
            ReflectionTestUtils.setField(member, "id", (long) i);
            testMembers.add(member);
        }

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
    @DisplayName("방 입장 동시성 테스트 - 정원 초과 시 일부만 입장 성공")
    void joinRoomConcurrencyTest() {
        // given
        Room spyRoom = spy(testRoom);
        when(roomRepository.findRoomById(1L)).thenReturn(Optional.of(spyRoom));

        for (Member member : testMembers) {
            when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
            lenient().when(blacklistService.isBlacklisted(1L, member.getId())).thenReturn(false);
        }

        when(spyRoom.validatePassword(any())).thenReturn(true);

        when(spyRoom.isFull()).thenReturn(false, false, true);
        lenient().when(spyRoom.hasPlayer(anyLong())).thenReturn(false);

        // when & then
        roomService.joinRoom(1L, 2L, null);

        roomService.joinRoom(1L, 3L, null);

        assertThatThrownBy(() -> roomService.joinRoom(1L, 4L, null))
                .isInstanceOf(ServiceException.class);

        verify(spyRoom, times(2)).addPlayer(anyLong());
        verify(spyRoom, times(3)).isFull();
    }

    @Test
    @DisplayName("방장 퇴장 시 소유권 경쟁 상태 테스트")
    void ownerLeaveWithConcurrentPlayerLeaveTest() {
        // given
        Room spyRoom = spy(testRoom);

        Set<Long> players = new HashSet<>();
        players.add(1L);
        players.add(2L);
        players.add(3L);
        ReflectionTestUtils.setField(spyRoom, "players", new HashSet<>(players));

        when(roomRepository.findRoomById(1L)).thenReturn(Optional.of(spyRoom));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testOwner));

        lenient().when(memberRepository.findById(2L)).thenReturn(Optional.of(testMembers.get(0)));

        when(spyRoom.isOwner(1L)).thenReturn(true);
        when(spyRoom.getId()).thenReturn(1L);

        Set<Long> reducedPlayers = new HashSet<>();
        reducedPlayers.add(2L);

        doAnswer(invocation -> {
            Long memberId = invocation.getArgument(0);
            if (memberId == 1L) {
                lenient().when(spyRoom.getPlayers()).thenReturn(reducedPlayers);
            }
            return null;
        }).when(spyRoom).removePlayer(anyLong());

        when(spyRoom.getPlayers()).thenReturn(players);

        // when
        roomService.leaveRoom(1L, 1L);

        // then
        verify(spyRoom).removePlayer(1L);
    }

    @Test
    @DisplayName("준비 상태 변경 충돌 테스트")
    void toggleReadyConcurrencyTest() {
        // given
        Room spyRoom = spy(testRoom);
        Set<Long> readyPlayers = new HashSet<>();
        ReflectionTestUtils.setField(spyRoom, "readyPlayers", readyPlayers);

        when(roomRepository.findRoomById(1L)).thenReturn(Optional.of(spyRoom));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(testMembers.get(0)));
        when(spyRoom.hasPlayer(2L)).thenReturn(true);

        Set<Long> emptySet = new HashSet<>();
        Set<Long> readySet = new HashSet<>();
        readySet.add(2L);

        when(spyRoom.getReadyPlayers())
                .thenReturn(emptySet)
                .thenReturn(readySet);

        // when
        roomService.toggleReady(1L, 2L);

        roomService.toggleReady(1L, 2L);

        // then
        verify(spyRoom, times(1)).playerReady(2L);
        verify(spyRoom, times(1)).playerUnready(2L);
    }

    @Test
    @DisplayName("방 입장과 블랙리스트 추가 경쟁 테스트")
    void joinRoomWithBlacklistAddConcurrencyTest() {
        // given
        Room spyRoom = spy(testRoom);

        when(roomRepository.findRoomById(1L)).thenReturn(Optional.of(spyRoom));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(testMembers.get(0)));

        when(blacklistService.isBlacklisted(1L, 2L))
                .thenReturn(false)
                .thenReturn(true);

        when(spyRoom.validatePassword(any())).thenReturn(true);
        when(spyRoom.isFull()).thenReturn(false);
        when(spyRoom.hasPlayer(2L)).thenReturn(false);

        // when & then
        roomService.joinRoom(1L, 2L, null);

        assertThatThrownBy(() -> roomService.joinRoom(1L, 2L, null))
                .isInstanceOf(ServiceException.class);

        verify(spyRoom, times(1)).addPlayer(2L);
    }
}