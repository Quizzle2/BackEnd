package com.ll.quizzle.domain.friend.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FriendRequestStatus {
    PENDING("요청 대기중"),
    ACCEPTED("요청 수락됨"),
    REJECTED("요청 거절됨");

    private final String description;

    public static FriendRequestStatus from(String status) {
        return FriendRequestStatus.valueOf(status.toUpperCase());
    }
}
