package com.ll.quizzle.domain.room.entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.room.type.AnswerType;
import com.ll.quizzle.domain.room.type.Difficulty;
import com.ll.quizzle.domain.room.type.MainCategory;
import com.ll.quizzle.domain.room.type.RoomStatus;
import com.ll.quizzle.domain.room.type.SubCategory;
import static com.ll.quizzle.global.exceptions.ErrorCode.MIN_PLAYER_COUNT_NOT_MET;
import static com.ll.quizzle.global.exceptions.ErrorCode.NOT_ALL_PLAYERS_READY;
import static com.ll.quizzle.global.exceptions.ErrorCode.NOT_ROOM_OWNER;
import static com.ll.quizzle.global.exceptions.ErrorCode.PLAYER_LEFT_DURING_START;
import com.ll.quizzle.global.jpa.entity.BaseTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseTime {
    
    @Column(nullable = false)
    private String title;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Member owner;
    
    @Column(nullable = false)
    private int capacity;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "main_category", nullable = false)
    private MainCategory mainCategory;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sub_category", nullable = false)
    private SubCategory subCategory;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "answer_type", nullable = false)
    private AnswerType answerType;
    
    @Column(name = "problem_count", nullable = false)
    private int problemCount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private Difficulty difficulty;
    
    @Column
    private String passwordHash;
    
    @Column(nullable = false)
    private boolean isPrivate = false;
    
    @Version
    private Long version;
    
    @ElementCollection
    @CollectionTable(name = "room_players")
    private final Set<Long> players = Collections.synchronizedSet(new HashSet<>());
    
    @ElementCollection
    @CollectionTable(name = "room_ready_players")
    private final Set<Long> readyPlayers = Collections.synchronizedSet(new HashSet<>());
    
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<RoomBlacklist> blacklist = Collections.synchronizedSet(new HashSet<>());
    
    @Transient
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Builder
    private Room(String title, Member owner, int capacity, MainCategory mainCategory,
                SubCategory subCategory, AnswerType answerType, int problemCount, 
                Difficulty difficulty, String password) {
        this.title = title;
        this.owner = owner;
        this.capacity = capacity;
        this.mainCategory = mainCategory;
        this.subCategory = subCategory;
        this.answerType = answerType;
        this.problemCount = problemCount;
        this.difficulty = difficulty;
        
        if (password != null && !password.isEmpty()) {
            this.passwordHash = passwordEncoder.encode(password);
            this.isPrivate = true;
        } else {
            this.passwordHash = null;
            this.isPrivate = false;
        }
        
        this.status = RoomStatus.WAITING;
        this.players.add(owner.getId());
    }
    
    public boolean validatePassword(String inputPassword) {
        log.debug("=== 비밀번호 검증 디버깅 ===");
        log.debug("방 ID: {}, isPrivate: {}, passwordHash: {}", 
                 this.getId(), isPrivate, (passwordHash != null ? "있음" : "null"));
        log.debug("입력된 비밀번호: {}", 
                 (inputPassword != null ? (inputPassword.isEmpty() ? "빈 문자열" : "입력됨") : "null"));

        if (!isPrivate) {
            log.debug("결과: 공개방이므로 비밀번호 검증 통과");
            return true;
        }
        
        if (passwordHash == null) {
            log.debug("결과: 비공개방이지만 비밀번호 해시가 null이므로 검증 실패");
            return false;
        }
        
        if (inputPassword == null || inputPassword.isEmpty()) {
            log.debug("결과: 비밀번호가 없거나 빈 문자열이므로 검증 실패");
            return false;
        }
        
        boolean matches = passwordEncoder.matches(inputPassword, passwordHash);
        log.debug("결과: 비밀번호 일치 여부: {}", matches);
        return matches;
    }
    
    public boolean isOwner(Long memberId) {
        return owner.getId().equals(memberId);
    }
    
    public boolean isFull() {
        return players.size() >= capacity;
    }
    
    public boolean hasPlayer(Long memberId) {
        return players.contains(memberId);
    }
    
    public void addPlayer(Long memberId) {
        players.add(memberId);
    }
    
    public void removePlayer(Long memberId) {
        players.remove(memberId);
        readyPlayers.remove(memberId);
    }
    
    public void playerReady(Long memberId) {
        if (hasPlayer(memberId) && !isOwner(memberId)) {
            readyPlayers.add(memberId);
        }
    }
    
    public void playerUnready(Long memberId) {
        if (!isOwner(memberId)) {
            readyPlayers.remove(memberId);
        }
    }
    
    public boolean isAllPlayersReady() {
        if (players.size() == 1 && isOwner(players.iterator().next())) {
            return true;
        }
        
        long nonOwnerCount = players.stream()
            .filter(playerId -> !isOwner(playerId))
            .count();
        return nonOwnerCount == readyPlayers.size();
    }
    
    public void startGame(Long memberId) {
        if (!isOwner(memberId)) {
            throw NOT_ROOM_OWNER.throwServiceException();
        }
        
        if (this.players.isEmpty()) {
            throw MIN_PLAYER_COUNT_NOT_MET.throwServiceException();
        }
        
        if (!isAllPlayersReady()) {
            throw NOT_ALL_PLAYERS_READY.throwServiceException();
        }

        /**
         * 기존에 플레이어가 방을 떠나거나 연결이 끊기면 자동으로 처리를 해주긴 하지만,
         * 동시성 문제로 readyPlayers 에서는 제거되지 않고 players 에만 남아있을 수 있기에
         * 게임이 시작될 때 중복된 방어 코드를 추가하여 사용자 경험을 향상시키는 목적
         */
        Set<Long> currentPlayers = new HashSet<>(this.players);
        Set<Long> invalidPlayers = new HashSet<>();
        
        for (Long readyPlayerId : this.readyPlayers) {
            if (!currentPlayers.contains(readyPlayerId)) {
                invalidPlayers.add(readyPlayerId);
            }
        }
        
        if (!invalidPlayers.isEmpty()) {
            for (Long invalidPlayer : invalidPlayers) {
                this.readyPlayers.remove(invalidPlayer);
            }
            
            throw PLAYER_LEFT_DURING_START.throwServiceException();
        }
        
        this.status = RoomStatus.IN_GAME;
    }
    
    public void endGame() {
        this.status = RoomStatus.WAITING;
        readyPlayers.clear();
    }
    
    public void addToBlacklist(Member member) {
        RoomBlacklist blacklist = RoomBlacklist.builder()
                .room(this)
                .member(member)
                .build();
        this.blacklist.add(blacklist);
        
        if (hasPlayer(member.getId())) {
            removePlayer(member.getId());
        }
    }
    
    public void changeOwner(Member newOwner) {
        this.owner = newOwner;
        
        readyPlayers.remove(newOwner.getId());
    }
    
    public void updateRoom(String title, Integer capacity, Difficulty difficulty, 
                         MainCategory mainCategory, SubCategory subCategory, 
                         String password, Boolean isPrivate) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title;
        }
        
        if (capacity != null && capacity > 0 && capacity <= 8) {
            this.capacity = capacity;
        }
        
        if (difficulty != null) {
            this.difficulty = difficulty;
        }
        
        if (mainCategory != null) {
            this.mainCategory = mainCategory;
        }
        
        if (subCategory != null) {
            this.subCategory = subCategory;
        }
        
        if (isPrivate != null) {
            if (isPrivate) {
                if (password != null && !password.isEmpty()) {
                    this.passwordHash = passwordEncoder.encode(password);
                    this.isPrivate = true;
                } else {
                    log.debug("비공개 방으로 설정하려 했으나 비밀번호가 없어 공개 방으로 설정됩니다.");
                    this.passwordHash = null;
                    this.isPrivate = false;
                }
            } else {
                this.passwordHash = null;
                this.isPrivate = false;
            }
        } else if (password != null) {
            if (!password.isEmpty()) {
                this.passwordHash = passwordEncoder.encode(password);
                this.isPrivate = true;
            } else {
                this.passwordHash = null;
                this.isPrivate = false;
            }
        }
    }
}

