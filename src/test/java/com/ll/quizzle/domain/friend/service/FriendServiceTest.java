package com.ll.quizzle.domain.friend.service;

import static com.ll.quizzle.global.exceptions.ErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.avatar.repository.AvatarRepository;
import com.ll.quizzle.domain.avatar.repository.OwnedAvatarRepository;
import com.ll.quizzle.domain.friend.dto.response.FriendListResponse;
import com.ll.quizzle.domain.friend.dto.response.FriendOfferListResponse;
import com.ll.quizzle.domain.friend.dto.response.FriendOfferResponse;
import com.ll.quizzle.domain.friend.dto.response.FriendResponse;
import com.ll.quizzle.domain.friend.type.FriendRequestStatus;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.factory.TestMemberFactory;
import com.ll.quizzle.global.exceptions.ErrorCode;
import com.ll.quizzle.global.exceptions.ServiceException;
import com.ll.quizzle.global.security.oauth2.repository.OAuthRepository;

@SpringBootTest
@Transactional
public class FriendServiceTest {
    @Autowired
    private FriendService friendService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OAuthRepository oAuthRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private OwnedAvatarRepository ownedAvatarRepository;

    private Member testMember;
    private Member secondMember;
    private Member thirdMember;

    @BeforeEach
    void setUp() {

        Avatar defaultAvatar = avatarRepository.findByFileName("새콩이")
            .orElseThrow(AVATAR_NOT_FOUND::throwServiceException);

        testMember = TestMemberFactory.createOAuthMember(
            "테스트유저1", "test1@email.com", "google", "1234",
            memberRepository, oAuthRepository, avatarRepository, ownedAvatarRepository
        );

        secondMember = TestMemberFactory.createOAuthMember(
            "테스트유저2", "test2@email.com", "google", "2345",
            memberRepository, oAuthRepository, avatarRepository, ownedAvatarRepository
        );

        thirdMember = TestMemberFactory.createOAuthMember(
            "테스트유저3", "test3@email.com", "google", "3456",
            memberRepository, oAuthRepository, avatarRepository, ownedAvatarRepository
        );
    }

    @Test
    @DisplayName("친구 요청 - 성공")
    void testSendFriendOffer() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();

        // When
        FriendResponse friendResponse = friendService.sendFriendOffer(testMemberId, secondMemberId);

        // Then
        assertThat(friendResponse)
            .extracting("fromMemberId", "toMemberId", "status")
            .containsExactly(testMemberId, secondMemberId, FriendRequestStatus.PENDING.name());
    }

    @Test
    @DisplayName("친구 요청 - 실패 : 본인에게 요청을 보낸 경우")
    void testSendFriendOfferSelf() {
        // Given
        long testMemberId = testMember.getId();

        // When & Then
        assertThatThrownBy(() -> friendService.sendFriendOffer(testMemberId, testMemberId))
            .isInstanceOf(ServiceException.class)
            .extracting("msg")
            .isEqualTo(ErrorCode.FRIEND_REQUEST_NOT_YOURSELF.getMessage());
    }

    @Test
    @DisplayName("친구 요청 - 실패 : 이미 친구인 경우")
    void testSendFriendOfferAlreadyFriend() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();

        // When
        friendService.sendFriendOffer(testMemberId, secondMemberId);
        friendService.handleFriendOffer(secondMemberId, testMemberId, FriendRequestStatus.ACCEPTED);

        // Then
        assertThatThrownBy(() -> friendService.sendFriendOffer(testMemberId, secondMemberId))
            .isInstanceOf(ServiceException.class)
            .extracting("msg")
            .isEqualTo(ErrorCode.FRIEND_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("친구 요청 - 실패 : 이미 요청을 보낸 경우")
    void testSendFriendOfferAlreadyExists() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();

        // When
        friendService.sendFriendOffer(testMemberId, secondMemberId);

        // Then
        assertThatThrownBy(() -> friendService.sendFriendOffer(testMemberId, secondMemberId))
            .isInstanceOf(ServiceException.class)
            .extracting("msg")
            .isEqualTo(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("친구 요청 수락 - 성공")
    void testAcceptFriendOffer() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();

        // When
        friendService.sendFriendOffer(testMemberId, secondMemberId);
        FriendOfferResponse friendOfferResponse = friendService.handleFriendOffer(secondMemberId, testMemberId,
            FriendRequestStatus.ACCEPTED);

        // Then
        assertThat(friendOfferResponse)
            .extracting("memberId", "nickname", "status")
            .containsExactly(testMemberId, testMember.getNickname(), FriendRequestStatus.ACCEPTED.name());
    }

    @Test
    @DisplayName("친구 요청 수락 - 실패 : 친구 요청이 없는 경우")
    void testAcceptFriendOfferNotFound() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();

        // When & Then
        assertThatThrownBy(
            () -> friendService.handleFriendOffer(secondMemberId, testMemberId, FriendRequestStatus.ACCEPTED))
            .isInstanceOf(ServiceException.class)
            .extracting("msg")
            .isEqualTo(ErrorCode.FRIEND_REQUEST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("친구 요청 수락 - 성공 : 양방향 친구 관계 확인")
    void testFriendRelationship() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();

        // When
        friendService.sendFriendOffer(testMemberId, secondMemberId);
        friendService.handleFriendOffer(secondMemberId, testMemberId, FriendRequestStatus.ACCEPTED);

        // Then
        List<FriendListResponse> friendList = friendService.getFriendList(testMemberId);
        List<FriendListResponse> targetFriendList = friendService.getFriendList(secondMemberId);

        assertThat(friendList).hasSize(1);
        assertThat(targetFriendList).hasSize(1);

        assertThat(friendList.get(0))
            .extracting("memberId", "nickname", "level")
            .containsExactly(
                secondMemberId, secondMember.getNickname(), secondMember.getLevel()
            );

        assertThat(targetFriendList.get(0))
            .extracting("memberId", "nickname", "level")
            .containsExactly(
                testMemberId, testMember.getNickname(), testMember.getLevel()
            );
    }

    @Test
    @DisplayName("친구 요청 거절 - 성공")
    void testRejectFriendOffer() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();

        // When
        friendService.sendFriendOffer(testMemberId, secondMemberId);
        FriendOfferResponse friendOfferResponse = friendService.handleFriendOffer(secondMemberId, testMemberId,
            FriendRequestStatus.REJECTED);

        // Then
        assertThat(friendOfferResponse)
            .extracting("memberId", "nickname", "status")
            .containsExactly(testMemberId, testMember.getNickname(), FriendRequestStatus.REJECTED.name());
    }

    @Test
    @DisplayName("친구 요청 거절 - 실패 : 친구 요청이 없는 경우")
    void testRejectFriendOfferNotFound() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();

        // When & Then
        assertThatThrownBy(
            () -> friendService.handleFriendOffer(secondMemberId, testMemberId, FriendRequestStatus.REJECTED))
            .isInstanceOf(ServiceException.class)
            .extracting("msg")
            .isEqualTo(ErrorCode.FRIEND_REQUEST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("친구 요청 거절 - 거절 후 다시 요청 성공")
    void testRejectFriendOfferAndAccept() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();

        friendService.sendFriendOffer(testMemberId, secondMemberId);
        friendService.handleFriendOffer(secondMemberId, testMemberId, FriendRequestStatus.REJECTED);

        // When
        FriendResponse friendResponse = friendService.sendFriendOffer(testMemberId, secondMemberId);

        // Then
        assertThat(friendResponse)
            .extracting("fromMemberId", "toMemberId", "status")
            .containsExactly(testMemberId, secondMemberId, FriendRequestStatus.PENDING.name());
    }

    @Test
    @DisplayName("친구 요청 목록 조회 - 성공")
    void testGetFriendOfferList() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();

        // When
        friendService.sendFriendOffer(testMemberId, secondMemberId);
        List<FriendOfferListResponse> friendOfferList = friendService.getFriendOfferList(secondMemberId);

        // Then
        assertThat(friendOfferList)
            .hasSize(1)
            .extracting("memberId", "nickname")
            .containsExactly(
                tuple(testMemberId, testMember.getNickname())
            );
    }

    @Test
    @DisplayName("친구 요청 목록 조회 - 여러 사용자로부터 요청")
    void testGetFriendOfferListMultipleRequests() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();
        long thirdMemberId = thirdMember.getId();

        // When
        friendService.sendFriendOffer(testMemberId, secondMemberId);
        friendService.sendFriendOffer(thirdMemberId, secondMemberId);
        List<FriendOfferListResponse> friendOfferList = friendService.getFriendOfferList(secondMemberId);

        // Then
        assertThat(friendOfferList)
            .hasSize(2)
            .extracting("memberId", "nickname")
            .containsExactlyInAnyOrder(
                tuple(testMemberId, testMember.getNickname()),
                tuple(thirdMemberId, thirdMember.getNickname())
            );
    }

    @Test
    @DisplayName("친구 목록 조회 - 성공")
    void testGetFriendList() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();

        // When
        friendService.sendFriendOffer(testMemberId, secondMemberId);
        friendService.handleFriendOffer(secondMemberId, testMemberId, FriendRequestStatus.ACCEPTED);
        List<FriendListResponse> friendList = friendService.getFriendList(testMemberId);
        List<FriendListResponse> targetFriendList = friendService.getFriendList(secondMemberId);

        // Then
        assertThat(friendList)
            .hasSize(1)
            .extracting("memberId", "nickname", "level")
            .containsExactly(
                tuple(secondMemberId, secondMember.getNickname(), secondMember.getLevel())
            );

        assertThat(targetFriendList)
            .hasSize(1)
            .extracting("memberId", "nickname", "level")
            .containsExactly(
                tuple(testMemberId, testMember.getNickname(), testMember.getLevel())
            );
    }

    @Test
    @DisplayName("친구 목록 조회 - 친구가 없는 경우")
    void testGetFriendListEmpty() {
        // Given
        long testMemberId = testMember.getId();

        // When
        List<FriendListResponse> friendList = friendService.getFriendList(testMemberId);

        // Then
        assertThat(friendList).isEmpty();
    }

    @Test
    @DisplayName("친구 목록 조회 - 여러 친구")
    void testGetFriendListMultiple() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();
        long thirdMemberId = thirdMember.getId();

        // When
        friendService.sendFriendOffer(testMemberId, secondMemberId);
        friendService.handleFriendOffer(secondMemberId, testMemberId, FriendRequestStatus.ACCEPTED);

        friendService.sendFriendOffer(testMemberId, thirdMemberId);
        friendService.handleFriendOffer(thirdMemberId, testMemberId, FriendRequestStatus.ACCEPTED);

        List<FriendListResponse> friendList = friendService.getFriendList(testMemberId);

        // Then
        assertThat(friendList)
            .hasSize(2)
            .extracting("memberId", "nickname")
            .containsExactlyInAnyOrder(
                tuple(secondMemberId, secondMember.getNickname()),
                tuple(thirdMemberId, thirdMember.getNickname())
            );
    }

    @Test
    @DisplayName("친구 삭제 - 성공")
    void testDeleteFriend() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();

        friendService.sendFriendOffer(testMemberId, secondMemberId);
        friendService.handleFriendOffer(secondMemberId, testMemberId, FriendRequestStatus.ACCEPTED);

        List<FriendListResponse> friendsBefore = friendService.getFriendList(testMemberId);
        assertThat(friendsBefore).hasSize(1);

        // When
        friendService.deleteFriend(testMemberId, secondMemberId);

        // Then
        List<FriendListResponse> friendsAfter = friendService.getFriendList(testMemberId);
        assertThat(friendsAfter).isEmpty();

        List<FriendListResponse> targetFriendsAfter = friendService.getFriendList(secondMemberId);
        assertThat(targetFriendsAfter).isEmpty();
    }

    @Test
    @DisplayName("친구 삭제 - 성공 : 삭제 후 재요청")
    void testSendFriendOfferAfterDelete() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();

        friendService.sendFriendOffer(testMemberId, secondMemberId);
        friendService.handleFriendOffer(secondMemberId, testMemberId, FriendRequestStatus.ACCEPTED);
        friendService.deleteFriend(testMemberId, secondMemberId);

        // When
        FriendResponse response = friendService.sendFriendOffer(testMemberId, secondMemberId);

        // Then
        assertThat(response)
            .extracting("fromMemberId", "toMemberId", "status")
            .containsExactly(testMemberId, secondMemberId, FriendRequestStatus.PENDING.name());
    }

    @Test
    @DisplayName("친구 삭제 - 실패 : 친구가 아닌 경우")
    void testDeleteFriendNotFound() {
        // Given
        long testMemberId = testMember.getId();
        long secondMemberId = secondMember.getId();

        // When & Then
        assertThatThrownBy(() -> friendService.deleteFriend(testMemberId, secondMemberId))
            .isInstanceOf(ServiceException.class)
            .extracting("msg")
            .isEqualTo(ErrorCode.FRIEND_NOT_FOUND.getMessage());
    }
}