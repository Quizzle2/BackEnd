package com.ll.quizzle.domain.friend.service;

import com.ll.quizzle.domain.friend.dto.response.FriendListResponse;
import com.ll.quizzle.domain.friend.dto.response.FriendOfferResponse;
import com.ll.quizzle.domain.friend.dto.response.FriendOfferListResponse;
import com.ll.quizzle.domain.friend.dto.response.FriendResponse;
import com.ll.quizzle.domain.friend.entity.Friend;
import com.ll.quizzle.domain.friend.entity.FriendOffer;
import com.ll.quizzle.domain.friend.repository.FriendOfferRepository;
import com.ll.quizzle.domain.friend.repository.FriendRepository;
import com.ll.quizzle.domain.friend.type.FriendRequestStatus;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ll.quizzle.global.exceptions.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class FriendService {
    private final FriendRepository friendRepository;
    private final FriendOfferRepository friendOfferRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public FriendResponse sendFriendOffer(long actorId, long memberId) {
        Member member = getMemberById(actorId);
        Member friend = getMemberById(memberId);

        validateFriendRequest(member, friend);

        // 이미 요청을 보낸 경우
        if (isSendOffer(member, friend)) {
            throw FRIEND_REQUEST_ALREADY_EXISTS.throwServiceException();
        }

        FriendOffer newFriendOffer = FriendOffer.create(member, friend);
        FriendOffer save = friendOfferRepository.save(newFriendOffer);

        return FriendResponse.from(save);
    }

    @Transactional
    public FriendOfferResponse handleFriendOffer(long actorId, long memberId, FriendRequestStatus status) {
        Member member = getMemberById(actorId);
        Member friend = getMemberById(memberId);

        validateFriendRequest(member, friend);

        // 요청이 없을 경우 에러발생
        FriendOffer friendOffer = friendOfferRepository.findByFromMemberAndToMember(friend, member)
                .orElseThrow(FRIEND_REQUEST_NOT_FOUND::throwServiceException);

        if (status == FriendRequestStatus.ACCEPTED) {
            friendOffer.changeStatus(FriendRequestStatus.ACCEPTED);

            // 친구 요청 수락시 친구 테이블에 양방향으로 저장 (A->B, B->A)
            Friend friendAtoB = Friend.create(member, friend);
            Friend friendBtoA = Friend.create(friend, member);

            friendRepository.save(friendAtoB);
            friendRepository.save(friendBtoA);
        } else if (status == FriendRequestStatus.REJECTED) {
            friendOffer.changeStatus(FriendRequestStatus.REJECTED);
        }

        friendOfferRepository.delete(friendOffer);

        return FriendOfferResponse.from(friendOffer);
    }

    public List<FriendOfferListResponse> getFriendOfferList(long actorId) {
        Member member = getMemberById(actorId);
        List<FriendOffer> friendOfferList = friendOfferRepository.findAllByToMemberOrderByCreateDateAsc(member);

        return friendOfferList.stream()
                .map(FriendOfferListResponse::from)
                .toList();
    }

    public List<FriendListResponse> getFriendList(long actorId) {
        Member member = getMemberById(actorId);
        List<Friend> friendList = friendRepository.findAllByMemberOrderByFriendNicknameAsc(member);

        return friendList.stream()
                .map(FriendListResponse::from)
                .toList();
    }

    @Transactional
    public void deleteFriend(long actorId, long memberId) {
        Member member = getMemberById(actorId);
        Member friend = getMemberById(memberId);

        // 친구가 아닌 경우
        if (!isFriend(member, friend)) {
            throw FRIEND_NOT_FOUND.throwServiceException();
        }

        // 친구 삭제
        Friend friendAtoB = getFriendRelation(member, friend);
        Friend friendBtoA = getFriendRelation(friend, member);

        friendRepository.delete(friendAtoB);
        friendRepository.delete(friendBtoA);
    }


    private Friend getFriendRelation(Member member, Member friend) {
        return friendRepository.findByMemberAndFriend(member, friend)
                .orElseThrow(FRIEND_NOT_FOUND::throwServiceException);
    }

    private Member getMemberById(long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MEMBER_NOT_FOUND::throwServiceException);
    }

    private void validateFriendRequest(Member member, Member friend) {
        // 본인에게 친구 요청을 보낼 수 없음
        if (member.equals(friend)) {
            throw FRIEND_REQUEST_NOT_YOURSELF.throwServiceException();
        }

        // 이미 친구인 경우
        if (isFriend(member, friend)) {
            throw FRIEND_ALREADY_EXISTS.throwServiceException();
        }
    }

    private boolean isSendOffer(Member fromMember, Member toMember) {
        return friendOfferRepository.existsByFromMemberAndToMember(fromMember, toMember) ||
                friendOfferRepository.existsByFromMemberAndToMember(toMember, fromMember);
    }

    private boolean isFriend(Member member, Member friend) {
        return friendRepository.existsByMemberAndFriend(member, friend) ||
                friendRepository.existsByMemberAndFriend(friend, member);
    }
}
