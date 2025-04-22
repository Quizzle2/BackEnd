package com.ll.quizzle.global.initdata;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.avatar.repository.AvatarRepository;
import com.ll.quizzle.domain.avatar.type.AvatarTemplate;

import lombok.RequiredArgsConstructor;

@Profile("prod")
@Configuration
@RequiredArgsConstructor
public class ProdInitData {

    private final AvatarRepository avatarRepository;

    @Bean
    public ApplicationRunner prodInitDataRunner() {
        return args -> initAvatars();
    }

    @Transactional
    public void initAvatars() {
        for (AvatarTemplate template : AvatarTemplate.values()) {
            if (!avatarRepository.existsByFileName(template.fileName)) {
                Avatar avatar = template.toEntity();
                avatarRepository.save(avatar);
            }
        }
    }
}
