package com.ll.quizzle.domain.room.entity;

import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomBlacklist extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Room room;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;
    
    @Builder
    private RoomBlacklist(Room room, Member member) {
        this.room = room;
        this.member = member;
    }
} 