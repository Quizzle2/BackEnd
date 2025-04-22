package com.ll.quizzle.domain.room.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.room.entity.Room;
import com.ll.quizzle.domain.room.entity.RoomBlacklist;
import com.ll.quizzle.domain.room.repository.RoomBlacklistRepository;
import com.ll.quizzle.domain.room.repository.RoomRepository;
import static com.ll.quizzle.global.exceptions.ErrorCode.MEMBER_ALREADY_BLACKLISTED;
import static com.ll.quizzle.global.exceptions.ErrorCode.MEMBER_NOT_FOUND;
import static com.ll.quizzle.global.exceptions.ErrorCode.ROOM_NOT_FOUND;
import static com.ll.quizzle.global.exceptions.ErrorCode.ROOM_OWNER_BLACKLIST_FORBIDDEN;
import com.ll.quizzle.global.redis.lock.DistributedLock;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomBlacklistService {
    private final RoomBlacklistRepository blacklistRepository;
    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository;
    
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void addToBlacklist(Long roomId, Long memberId) {
        Room room = findRoomOrThrow(roomId);
        Member member = findMemberOrThrow(memberId);
                
        if (room.isOwner(memberId)) {
            throw ROOM_OWNER_BLACKLIST_FORBIDDEN.throwServiceException();
        }
        
        addToBlacklistWithLock(room, member, memberId);
    }
    

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void removeFromBlacklist(Long roomId, Long memberId) {
        Room room = findRoomOrThrow(roomId);
        Member member = findMemberOrThrow(memberId);

        removeFromBlacklistWithLock(room, member);
    }

    public boolean isBlacklisted(Long roomId, Long memberId) {
        Room room = findRoomOrThrow(roomId);
        Member member = findMemberOrThrow(memberId);

        return blacklistRepository.existsByRoomAndMember(room, member);
    }

    /**
     * 블랙리스트에 등록된 멤버 목록 조회가 필요 할 경우를 대비해서 추가
     */
    public List<RoomBlacklist> getBlacklistedMembers(Long roomId) {
        Room room = findRoomOrThrow(roomId);

        return blacklistRepository.findByRoom(room);
    }

    @DistributedLock(key = "'room:' + #room.id", leaseTime = 10000)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void addToBlacklistWithLock(Room room, Member member, Long memberId) {

        if (blacklistRepository.existsByRoomAndMember(room, member)) {
            throw MEMBER_ALREADY_BLACKLISTED.throwServiceException();
        }

        RoomBlacklist blacklist = RoomBlacklist.builder()
                .room(room)
                .member(member)
                .build();

        blacklistRepository.save(blacklist);

        if (room.hasPlayer(memberId)) {
            room.removePlayer(memberId);
        }
    }

    @DistributedLock(key = "'room:' + #room.id", leaseTime = 10000)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void removeFromBlacklistWithLock(Room room, Member member) {
        blacklistRepository.deleteByRoomAndMember(room, member);
    }

    protected Room findRoomOrThrow(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(ROOM_NOT_FOUND::throwServiceException);
    }

    protected Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MEMBER_NOT_FOUND::throwServiceException);
    }
}