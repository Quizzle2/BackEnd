package com.ll.quizzle.domain.avatar.entity;

import com.ll.quizzle.domain.avatar.type.AvatarStatus;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Avatar extends BaseEntity {

    private String fileName;
    private String url;
    private int price;

    @Builder
    public Avatar(String fileName, String url, int price) {
        this.fileName = fileName;
        this.url = url;
        this.price = price;
    }


}
