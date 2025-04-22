package com.ll.quizzle.domain.room.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
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
import com.ll.quizzle.domain.room.dto.request.RoomCreateRequest;
import com.ll.quizzle.domain.room.dto.request.RoomUpdateRequest;
import com.ll.quizzle.domain.room.dto.response.RoomResponse;
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
class RoomServiceTest {

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

    private Member testOwner;
    private Room testRoom;
    private MockedStatic<TransactionSynchronizationManager> mockedTransactionManager;

    @BeforeEach
    void setUp() {
        mockedTransactionManager = mockStatic(TransactionSynchronizationManager.class);
        mockedTransactionManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                .thenAnswer(invocation -> null);

        testOwner = Member.create("테스트유저", "test@example.com", defaultAvatar);
        ReflectionTestUtils.setField(testOwner, "id", 1L);

        Room roomTemp = Room.builder()
                .title("테스트 방")
                .owner(testOwner)
                .capacity(4)
                .difficulty(Difficulty.NORMAL)
                .mainCategory(MainCategory.GENERAL_KNOWLEDGE)
                .subCategory(SubCategory.CULTURE)
                .answerType(AnswerType.MULTIPLE_CHOICE)
                .problemCount(10)
                .password("1234")
                .build();

        ReflectionTestUtils.setField(roomTemp, "id", 1L);
        ReflectionTestUtils.setField(roomTemp, "status", RoomStatus.WAITING);

        testRoom = spy(roomTemp);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);

        lenient().when(redisLockService.acquireLock(anyString(), anyLong(), anyLong())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        if (mockedTransactionManager != null) {
            mockedTransactionManager.close();
        }
    }

    @Test
    @DisplayName("방 생성 테스트")
    void createRoomTest() {
        // given
        RoomCreateRequest request = new RoomCreateRequest(
                "테스트 방",
                4,
                Difficulty.NORMAL,
                MainCategory.GENERAL_KNOWLEDGE,
                SubCategory.CULTURE,
                AnswerType.MULTIPLE_CHOICE,
                10,
                null,
                false
        );

        when(memberRepository.findById(1L)).thenReturn(Optional.of(testOwner));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        // when
        RoomResponse response = roomService.createRoom(1L, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("테스트 방");
        assertThat(response.difficulty()).isEqualTo(Difficulty.NORMAL);
        assertThat(response.mainCategory()).isEqualTo(MainCategory.GENERAL_KNOWLEDGE);
        assertThat(response.subCategory()).isEqualTo(SubCategory.CULTURE);

        verify(memberRepository).findById(1L);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    @DisplayName("비밀번호가 있는 방 생성 테스트")
    void createPrivateRoomTest() {
        // given
        RoomCreateRequest request = new RoomCreateRequest(
                "비밀방",
                4,
                Difficulty.NORMAL,
                MainCategory.GENERAL_KNOWLEDGE,
                SubCategory.CULTURE,
                AnswerType.MULTIPLE_CHOICE,
                10,
                "1234",
                true
        );

        when(memberRepository.findById(1L)).thenReturn(Optional.of(testOwner));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        // when
        RoomResponse response = roomService.createRoom(1L, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("테스트 방");
        assertThat(response.isPrivate()).isTrue();

        verify(memberRepository).findById(1L);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    @DisplayName("활성화된 방 목록 조회 테스트")
    void getActiveRoomsTest() {
        // given
        List<Room> rooms = Arrays.asList(testRoom);
        when(roomRepository.findByStatusNot(RoomStatus.FINISHED)).thenReturn(rooms);

        // when
        List<RoomResponse> responses = roomService.getActiveRooms();

        // then
        assertThat(responses).isNotNull().hasSize(1);
        assertThat(responses.getFirst().title()).isEqualTo("테스트 방");

        verify(roomRepository).findByStatusNot(RoomStatus.FINISHED);
    }

    @Test
    @DisplayName("방 입장 테스트 - 성공 (비밀번호 있다고 가정)")
    void joinRoomSuccessTest() {
        // given
        Member normalMember = Member.create("일반유저", "user@example.com", defaultAvatar);
        ReflectionTestUtils.setField(normalMember, "id", 2L);

        when(roomRepository.findRoomById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(normalMember));
        
        when(blacklistService.isBlacklisted(1L, 2L)).thenReturn(false);
        when(testRoom.validatePassword(any())).thenReturn(true);
        when(testRoom.isFull()).thenReturn(false);
        
        when(testRoom.hasPlayer(2L)).thenReturn(false);

        // when
        roomService.joinRoom(1L, 2L, null);

        // then
        verify(roomRepository).findRoomById(1L);
        verify(blacklistService).isBlacklisted(1L, 2L);
        verify(testRoom, atLeastOnce()).validatePassword(any());
        verify(testRoom).isFull();
        verify(testRoom).hasPlayer(2L);
        verify(testRoom).addPlayer(2L);
    }

    @Test
    @DisplayName("방 입장 테스트 - 비밀번호 오류")
    void joinRoomWrongPasswordTest() {
        // given
        when(roomRepository.findRoomById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(mock(Member.class)));
        
        when(blacklistService.isBlacklisted(1L, 2L)).thenReturn(false);
        when(testRoom.validatePassword(any())).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> roomService.joinRoom(1L, 2L, "wrong"))
                .isInstanceOf(ServiceException.class);

        // verify
        verify(roomRepository).findRoomById(1L);
        verify(blacklistService).isBlacklisted(1L, 2L);
        verify(testRoom, atLeastOnce()).validatePassword(any());
        verify(testRoom, never()).addPlayer(anyLong());
    }

    @Test
    @DisplayName("방 입장 테스트 - 블랙리스트")
    void joinRoomBlacklistTest() {
        // given
        when(roomRepository.findRoomById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(mock(Member.class)));
        
        when(blacklistService.isBlacklisted(1L, 2L)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> roomService.joinRoom(1L, 2L, null))
            .isInstanceOf(ServiceException.class);

        // verify
        verify(roomRepository).findRoomById(1L);
        verify(blacklistService).isBlacklisted(1L, 2L);
        verify(testRoom, never()).validatePassword(any());
        verify(testRoom, never()).addPlayer(anyLong());
    }

    @Test
    @DisplayName("방 퇴장 테스트 - 일반 유저")
    void leaveRoomNormalUserTest() {
        // given
        Member normalMember = Member.create("일반유저", "user@example.com", defaultAvatar);
        ReflectionTestUtils.setField(normalMember, "id", 2L);

        Set<Long> emptySet = new HashSet<>();
        
        when(roomRepository.findRoomById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(normalMember));
        
        lenient().when(testRoom.isOwner(2L)).thenReturn(false);
        lenient().when(testRoom.getId()).thenReturn(1L);
        lenient().when(testRoom.getPlayers()).thenReturn(emptySet);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);

        // when
        roomService.leaveRoom(1L, 2L);

        // then
        verify(roomRepository).findRoomById(1L);
        verify(memberRepository).findById(2L);
        verify(testRoom).removePlayer(2L);
        verify(roomRepository, never()).delete(any(Room.class));
    }

    @Test
    @DisplayName("방 퇴장 테스트 - 방장 (다른 플레이어 X)")
    void leaveRoomOwnerWithoutPlayersTest() {
        // given
        Set<Long> emptyPlayerSet = new HashSet<>();
        
        when(roomRepository.findRoomById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testOwner));
        
        lenient().when(testRoom.isOwner(1L)).thenReturn(true);
        lenient().when(testRoom.getId()).thenReturn(1L);
        lenient().when(testRoom.getPlayers()).thenReturn(emptyPlayerSet);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);

        // when
        roomService.leaveRoom(1L, 1L);

        // then
        verify(roomRepository).findRoomById(1L);
        verify(memberRepository).findById(1L);
        verify(testRoom).removePlayer(1L);
    }

    @Test
    @DisplayName("방 퇴장 테스트 - 방장 (다른 플레이어 O)")
    void leaveRoomOwnerWithPlayersTest() {
        // given
        Set<Long> playerSet = new HashSet<>();
        playerSet.add(2L);
        
        Member newOwner = Member.create("새방장", "newowner@example.com", defaultAvatar);
        ReflectionTestUtils.setField(newOwner, "id", 2L);
        
        when(roomRepository.findRoomById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testOwner));
        
        lenient().when(testRoom.isOwner(1L)).thenReturn(true);
        lenient().when(testRoom.getId()).thenReturn(1L);
        lenient().when(testRoom.getPlayers()).thenReturn(playerSet);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);
        
        // TransactionSynchronization 내부에서 findMemberOrThrow 호출되지만 테스트 시에는 실행되지 않으므로
        // 여기서 검증 X

        // when
        roomService.leaveRoom(1L, 1L);

        // then
        verify(roomRepository).findRoomById(1L);
        verify(memberRepository).findById(1L);
        verify(testRoom).removePlayer(1L);
    }

    @Test
    @DisplayName("준비 상태 토글 테스트")
    void toggleReadyTest() {
        // given
        Member normalMember = Member.create("일반유저", "user@example.com", defaultAvatar);
        ReflectionTestUtils.setField(normalMember, "id", 2L);

        Set<Long> readyPlayers = new HashSet<>();

        when(roomRepository.findRoomById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(normalMember));
        when(testRoom.hasPlayer(2L)).thenReturn(true);
        when(testRoom.getReadyPlayers()).thenReturn(readyPlayers);

        // when
        roomService.toggleReady(1L, 2L);

        // then
        verify(roomRepository).findRoomById(1L);
        verify(memberRepository).findById(2L);
        verify(testRoom).playerReady(2L);
    }

    @Test
    @DisplayName("게임 시작 테스트 - 성공")
    void startGameSuccessTest() {
        // given
        Set<Long> players = new HashSet<>();
        players.add(1L);
        
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(testRoom.isOwner(anyLong())).thenReturn(true);
        when(testRoom.isAllPlayersReady()).thenReturn(true);
        when(testRoom.getPlayers()).thenReturn(players);
        when(valueOperations.get(anyString())).thenReturn(null);

        // when
        roomService.startGame(1L, 1L);

        // then
        verify(roomRepository).findById(1L);
        verify(testRoom, atLeastOnce()).isOwner(anyLong());
        verify(testRoom).startGame(1L);
    }

    @Test
    @DisplayName("게임 시작 테스트 - 방장 아님")
    void startGameNotOwnerTest() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(testRoom.isOwner(2L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> roomService.startGame(1L, 2L))
            .isInstanceOf(ServiceException.class);

        verify(roomRepository).findById(1L);
        verify(testRoom).isOwner(2L);
        verify(testRoom, never()).startGame(anyLong());
    }

    @Test
    @DisplayName("게임 시작 테스트 - 모두 준비 안됨")
    void startGameNotAllReadyTest() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(testRoom.isOwner(1L)).thenReturn(true);
        when(testRoom.isAllPlayersReady()).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> roomService.startGame(1L, 1L))
            .isInstanceOf(ServiceException.class);

        verify(roomRepository).findById(1L);
        verify(testRoom).isOwner(1L);
        verify(testRoom, atLeastOnce()).isAllPlayersReady();
        verify(testRoom, never()).startGame(anyLong());
    }

    @Test
    @DisplayName("방 업데이트 테스트 - 성공")
    void updateRoomSuccessTest() {
        // given
        RoomUpdateRequest request = new RoomUpdateRequest(
                "업데이트된 방 제목",
                5,
                Difficulty.HARD,
                MainCategory.HISTORY,
                SubCategory.WORLD_HISTORY,
                null,
                false
        );

        when(roomRepository.findRoomById(1L)).thenReturn(Optional.of(testRoom));
        when(testRoom.isOwner(1L)).thenReturn(true);
        when(testRoom.getStatus()).thenReturn(RoomStatus.WAITING);
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        // when
        RoomResponse response = roomService.updateRoom(1L, 1L, request);

        // then
        assertThat(response).isNotNull();
        
        verify(testRoom).updateRoom(
                eq("업데이트된 방 제목"),
                eq(5),
                eq(Difficulty.HARD),
                eq(MainCategory.HISTORY),
                eq(SubCategory.WORLD_HISTORY),
                any(),
                eq(Boolean.FALSE)
        );
        
        verify(roomRepository).save(testRoom);
    }

    @Test
    @DisplayName("방 업데이트 테스트 - 비공개 방으로 변경")
    void updateRoomToPrivateTest() {
        // given
        RoomUpdateRequest request = new RoomUpdateRequest(
                "비공개 방",
                4,
                Difficulty.NORMAL,
                MainCategory.GENERAL_KNOWLEDGE,
                SubCategory.CULTURE,
                "5678",
                true
        );

        when(roomRepository.findRoomById(1L)).thenReturn(Optional.of(testRoom));
        when(testRoom.isOwner(1L)).thenReturn(true);
        when(testRoom.getStatus()).thenReturn(RoomStatus.WAITING);
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        // when
        RoomResponse response = roomService.updateRoom(1L, 1L, request);

        // then
        assertThat(response).isNotNull();
        
        verify(testRoom).updateRoom(
                eq("비공개 방"),
                eq(4),
                eq(Difficulty.NORMAL),
                eq(MainCategory.GENERAL_KNOWLEDGE),
                eq(SubCategory.CULTURE),
                eq("5678"),
                eq(Boolean.TRUE)
        );
        
        verify(roomRepository).save(testRoom);
    }

    @Test
    @DisplayName("방 업데이트 테스트 - 방장이 아닌 경우 실패")
    void updateRoomNotOwnerTest() {
        // given
        RoomUpdateRequest request = new RoomUpdateRequest(
                "업데이트 실패할 제목",
                4,
                Difficulty.NORMAL,
                MainCategory.GENERAL_KNOWLEDGE,
                SubCategory.CULTURE,
                null,
                false
        );

        when(roomRepository.findRoomById(1L)).thenReturn(Optional.of(testRoom));
        when(testRoom.isOwner(2L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> roomService.updateRoom(1L, 2L, request))
                .isInstanceOf(ServiceException.class);
        
        verify(testRoom, never()).updateRoom(
                any(), any(), any(), any(), any(), any(), any()
        );
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("방 업데이트 테스트 - 게임 중인 경우 실패")
    void updateRoomDuringGameTest() {
        // given
        RoomUpdateRequest request = new RoomUpdateRequest(
                "게임 중 업데이트 실패",
                4,
                Difficulty.NORMAL,
                MainCategory.GENERAL_KNOWLEDGE,
                SubCategory.CULTURE,
                null,
                false
        );

        when(roomRepository.findRoomById(1L)).thenReturn(Optional.of(testRoom));
        when(testRoom.isOwner(1L)).thenReturn(true);
        when(testRoom.getStatus()).thenReturn(RoomStatus.IN_GAME);

        // when & then
        assertThatThrownBy(() -> roomService.updateRoom(1L, 1L, request))
                .isInstanceOf(ServiceException.class);
        
        verify(testRoom, never()).updateRoom(
                any(), any(), any(), any(), any(), any(), any()
        );
        verify(roomRepository, never()).save(any(Room.class));
    }
}