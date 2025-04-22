package com.ll.quizzle.domain.avatar.type;

import com.ll.quizzle.domain.avatar.entity.Avatar;

public enum AvatarTemplate {
    DEFAULT("새콩이", "https://quizzle-avatars.s3.ap-northeast-2.amazonaws.com/%EA%B8%B0%EB%B3%B8+%EC%95%84%EB%B0%94%ED%83%80.png", 0),
    NERDY("안경쓴 새콩이", "https://quizzle-avatars.s3.ap-northeast-2.amazonaws.com/%EC%95%88%EA%B2%BD%EC%93%B4+%EC%83%88%EC%BD%A9%EC%9D%B4.png", 300),
    SURFING("서핑하는 새콩이", "https://quizzle-avatars.s3.ap-northeast-2.amazonaws.com/%EC%84%9C%ED%95%91%ED%95%98%EB%8A%94+%EC%83%88%EC%BD%A9%EC%9D%B4.png", 400),
    GREETING("인사하는 새콩이", "https://quizzle-avatars.s3.ap-northeast-2.amazonaws.com/%EC%9D%B8%EC%82%AC%ED%95%98%EB%8A%94+%EC%83%88%EC%BD%A9%EC%9D%B4.png", 200),
    GUITARIST("기타리스트 새콩이", "https://quizzle-avatars.s3.ap-northeast-2.amazonaws.com/%EA%B8%B0%ED%83%80%EB%A6%AC%EC%8A%A4%ED%8A%B8+%EC%83%88%EC%BD%A9%EC%9D%B4.png", 500),
    READER("독서하는 새콩이", "https://quizzle-avatars.s3.ap-northeast-2.amazonaws.com/%EB%8F%85%EC%84%9C%ED%95%98%EB%8A%94+%EC%83%88%EC%BD%A9%EC%9D%B4.png", 350);

    public final String fileName;
    public final String url;
    public final int price;

    AvatarTemplate(String fileName, String url, int price) {
        this.fileName = fileName;
        this.url = url;
        this.price = price;
    }

    public Avatar toEntity() {
        return Avatar.builder()
            .fileName(fileName)
            .url(url)
            .price(price)
            .build();
    }
}
