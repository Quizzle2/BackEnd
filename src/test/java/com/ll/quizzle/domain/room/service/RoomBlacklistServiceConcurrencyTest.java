package com.ll.quizzle.domain.room.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.room.entity.Room;
import com.ll.quizzle.domain.room.entity.RoomBlacklist;
import com.ll.quizzle.domain.room.repository.RoomBlacklistRepository;
import com.ll.quizzle.domain.room.repository.RoomRepository;
import com.ll.quizzle.domain.room.type.Difficulty;
import com.ll.quizzle.domain.room.type.MainCategory;
import com.ll.quizzle.domain.room.type.SubCategory;
import com.ll.quizzle.global.exceptions.ServiceException;
import com.ll.quizzle.global.redis.lock.DistributedLockService;


@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class RoomBlacklistServiceConcurrencyTest {

    @Mock
    private RoomBlacklistRepository blacklistRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private DistributedLockService distributedLockService;

    @InjectMocks
    private RoomBlacklistService blacklistService;

    private Room testRoom;
    private Member testOwner;
    private Member targetMember;
    private Avatar defaultAvatar;
    private MockedStatic<TransactionSynchronizationManager> mockedTransactionManager;

    @BeforeEach
    void setUp() {
        mockedTransactionManager = mockStatic(TransactionSynchronizationManager.class);
        mockedTransactionManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
            .thenAnswer(invocation -> null);

        testOwner = Member.create("방장", "owner@example.com", defaultAvatar);
        ReflectionTestUtils.setField(testOwner, "id", 1L);

        targetMember = Member.create("차단대상", "target@example.com", defaultAvatar);
        ReflectionTestUtils.setField(targetMember, "id", 2L);

        testRoom = Room.builder()
            .title("테스트 방")
            .owner(testOwner)
            .capacity(10)
            .difficulty(Difficulty.NORMAL)
            .mainCategory(MainCategory.GENERAL_KNOWLEDGE)
            .subCategory(SubCategory.CULTURE)
            .build();
        ReflectionTestUtils.setField(testRoom, "id", 1L);
    }

    @AfterEach
    void tearDown() {
        if (mockedTransactionManager != null) {
            mockedTransactionManager.close();
        }
    }

    @Test
    @DisplayName("블랙리스트 추가 동시성 테스트 - 두 번째 추가는 예외 발생")
    void addToBlacklistConcurrencyTest() {
        // given
        Room spyRoom = spy(testRoom);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(spyRoom));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(targetMember));
        lenient().when(distributedLockService.acquireLock(anyString(), anyLong(), anyLong())).thenReturn(true);

        when(blacklistRepository.existsByRoomAndMember(any(Room.class), any(Member.class)))
            .thenReturn(false)
            .thenReturn(true);

        // when
        blacklistService.addToBlacklist(1L, 2L);

        // then
        assertThatThrownBy(() -> blacklistService.addToBlacklist(1L, 2L))
            .isInstanceOf(ServiceException.class);

        verify(blacklistRepository, times(1)).save(any(RoomBlacklist.class));
    }
}