package com.ll.quizzle.global.initdata;

import static com.ll.quizzle.global.exceptions.ErrorCode.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.avatar.entity.OwnedAvatar;
import com.ll.quizzle.domain.avatar.repository.AvatarRepository;
import com.ll.quizzle.domain.avatar.repository.OwnedAvatarRepository;
import com.ll.quizzle.domain.avatar.type.AvatarTemplate;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.member.type.Role;
import com.ll.quizzle.domain.system.service.SystemService;
import com.ll.quizzle.global.security.oauth2.entity.OAuth;
import com.ll.quizzle.global.security.oauth2.repository.OAuthRepository;

import lombok.RequiredArgsConstructor;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class BaseInitData {
    private final MemberRepository memberRepository;
    private final OAuthRepository oAuthRepository;
    private final AvatarRepository avatarRepository;
    private final OwnedAvatarRepository ownedAvatarRepository;
    private final SystemService systemService;

    @Autowired
    @Lazy
    private BaseInitData self;

    @Bean
    public ApplicationRunner baseInitDataRunner() {
        return args -> {
            self.avatarInit();
            self.init();
            self.adminInit();
        };
    }

    @Transactional
    public void init() {
        if (memberRepository.count() > 0) {
            return;
        }

        Avatar defaultAvatar = avatarRepository.findByFileName("새콩이")
            .orElseThrow(AVATAR_NOT_FOUND::throwServiceException);

        for (int i = 1; i <= 10; i++) {
            String nickname = "test" + i;
            String email = "test" + i + "@email.com";
            String provider = (i % 2 == 0) ? "google" : "kakao";

            Member member = Member.create(nickname, email, defaultAvatar);

            // ✅ 기본 아바타 소유 등록
            OwnedAvatar ownedAvatar = OwnedAvatar.create(member, defaultAvatar);
            member.addOwnedAvatar(ownedAvatar); // 양방향 연결

            memberRepository.save(member);
            oAuthRepository.save(OAuth.create(member, provider, String.valueOf(i)));
            ownedAvatarRepository.save(ownedAvatar); // 반드시 저장
        }
    }



    @Transactional
    public void adminInit() {
        if (memberRepository.findByEmail("admin@quizzle.com").isPresent()) return;
        if (memberRepository.findByEmail("member@quizzle.com").isPresent()) return;

        Avatar defaultAvatar = avatarRepository.findByFileName("새콩이")
            .orElseThrow(AVATAR_NOT_FOUND::throwServiceException);

        Member testAdmin = Member.create("admin", "admin@quizzle.com", defaultAvatar);
        testAdmin.changeRole(Role.ADMIN);
        OwnedAvatar adminAvatar = OwnedAvatar.create(testAdmin, defaultAvatar);
        testAdmin.addOwnedAvatar(adminAvatar);

        Member testMember = Member.create("member", "member@quizzle.com", defaultAvatar);
        OwnedAvatar memberAvatar = OwnedAvatar.create(testMember, defaultAvatar);
        testMember.addOwnedAvatar(memberAvatar);

        memberRepository.save(testAdmin);
        memberRepository.save(testMember);
        oAuthRepository.save(OAuth.create(testAdmin, "kakao", "51"));
        oAuthRepository.save(OAuth.create(testMember, "google", "52"));
        ownedAvatarRepository.save(adminAvatar);
        ownedAvatarRepository.save(memberAvatar);
    }



    @Transactional
    public void avatarInit() {
        for (AvatarTemplate template : AvatarTemplate.values()) {
            boolean exists = avatarRepository.existsByFileName(template.fileName);
            if (!exists) {
                avatarRepository.save(template.toEntity());
            }
        }
    }

}
