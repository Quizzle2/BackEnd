package com.ll.quizzle.domain.room.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.room.entity.Room;
import com.ll.quizzle.domain.room.entity.RoomBlacklist;
import com.ll.quizzle.domain.room.repository.RoomBlacklistRepository;
import com.ll.quizzle.domain.room.repository.RoomRepository;
import com.ll.quizzle.domain.room.type.RoomStatus;
import com.ll.quizzle.global.exceptions.ServiceException;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class RoomBlacklistServiceTest {

    @Mock
    private RoomRepository roomRepository;
    
    @Mock
    private RoomBlacklistRepository blacklistRepository;
    
    @Mock
    private MemberRepository memberRepository;

    @Mock
    private Avatar defaultAvatar;
    
    @InjectMocks
    private RoomBlacklistService blacklistService;
    
    private Room testRoom;
    private RoomBlacklist testBlacklist;
    private Member testOwner;
    private Member testUser;
    
    @BeforeEach
    void setUp() {
        testOwner = Member.create("테스트방장", "owner@example.com", defaultAvatar);
        testUser = Member.create("테스트유저", "user@example.com", defaultAvatar);
        
        ReflectionTestUtils.setField(testOwner, "id", 1L);
        ReflectionTestUtils.setField(testUser, "id", 2L);
        
        Room roomTemp = Room.builder()
                .title("테스트 방")
                .owner(testOwner)
                .capacity(4)
                .build();
        
        ReflectionTestUtils.setField(roomTemp, "id", 1L);
        ReflectionTestUtils.setField(roomTemp, "status", RoomStatus.WAITING);
        
        testRoom = spy(roomTemp);
        
        RoomBlacklist blacklistTemp = RoomBlacklist.builder()
                .room(testRoom)
                .member(testUser)
                .build();
        
        ReflectionTestUtils.setField(blacklistTemp, "id", 1L);
        
        testBlacklist = spy(blacklistTemp);
    }
    
    @Test
    @DisplayName("블랙리스트 추가 테스트 - 성공")
    void addToBlacklistSuccessTest() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(blacklistRepository.existsByRoomAndMember(testRoom, testUser)).thenReturn(false);
        when(testRoom.isOwner(2L)).thenReturn(false);
        when(blacklistRepository.save(any(RoomBlacklist.class))).thenReturn(testBlacklist);
        
        // when
        blacklistService.addToBlacklist(1L, 2L);
        
        // then
        verify(roomRepository).findById(1L);
        verify(memberRepository).findById(2L);
        verify(blacklistRepository).existsByRoomAndMember(testRoom, testUser);
        verify(testRoom).isOwner(2L);
        verify(blacklistRepository).save(any(RoomBlacklist.class));
    }
    
    @Test
    @DisplayName("블랙리스트 추가 테스트 - 방장 추가 시도")
    void addToBlacklistOwnerTest() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testOwner));
        when(testRoom.isOwner(1L)).thenReturn(true);
        
        // when & then
        assertThatThrownBy(() -> blacklistService.addToBlacklist(1L, 1L))
            .isInstanceOf(ServiceException.class);
        
        verify(roomRepository).findById(1L);
        verify(memberRepository).findById(1L);
        verify(testRoom).isOwner(1L);
        verify(blacklistRepository, never()).save(any(RoomBlacklist.class));
    }
    
    @Test
    @DisplayName("블랙리스트 추가 테스트 - 이미 존재")
    void addToBlacklistAlreadyExistsTest() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(blacklistRepository.existsByRoomAndMember(testRoom, testUser)).thenReturn(true);
        
        // when & then
        assertThatThrownBy(() -> blacklistService.addToBlacklist(1L, 2L))
            .isInstanceOf(ServiceException.class);
        
        verify(roomRepository).findById(1L);
        verify(memberRepository).findById(2L);
        verify(blacklistRepository).existsByRoomAndMember(testRoom, testUser);
        verify(blacklistRepository, never()).save(any(RoomBlacklist.class));
    }
    
    @Test
    @DisplayName("블랙리스트 제거 테스트 - 성공")
    void removeFromBlacklistSuccessTest() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(testUser));
        
        // when
        blacklistService.removeFromBlacklist(1L, 2L);
        
        // then
        verify(roomRepository).findById(1L);
        verify(memberRepository).findById(2L);
        verify(blacklistRepository).deleteByRoomAndMember(testRoom, testUser);
    }
    
    @Test
    @DisplayName("블랙리스트 확인 테스트 - 블랙리스트에 있음")
    void isBlacklistedTrueTest() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(blacklistRepository.existsByRoomAndMember(testRoom, testUser)).thenReturn(true);
        
        // when
        boolean result = blacklistService.isBlacklisted(1L, 2L);
        
        // then
        assertThat(result).isTrue();
        verify(roomRepository).findById(1L);
        verify(memberRepository).findById(2L);
        verify(blacklistRepository).existsByRoomAndMember(testRoom, testUser);
    }
    
    @Test
    @DisplayName("블랙리스트 확인 테스트 - 블랙리스트에 없음")
    void isBlacklistedFalseTest() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(blacklistRepository.existsByRoomAndMember(testRoom, testUser)).thenReturn(false);
        
        // when
        boolean result = blacklistService.isBlacklisted(1L, 2L);
        
        // then
        assertThat(result).isFalse();
        verify(roomRepository).findById(1L);
        verify(memberRepository).findById(2L);
        verify(blacklistRepository).existsByRoomAndMember(testRoom, testUser);
    }
    
    @Test
    @DisplayName("블랙리스트 멤버 목록 조회 테스트")
    void getBlacklistedMembersTest() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(blacklistRepository.findByRoom(testRoom)).thenReturn(Arrays.asList(testBlacklist));
        
        // when
        List<RoomBlacklist> result = blacklistService.getBlacklistedMembers(1L);
        
        // then
        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.get(0)).isEqualTo(testBlacklist);
        
        verify(roomRepository).findById(1L);
        verify(blacklistRepository).findByRoom(testRoom);
    }
} 