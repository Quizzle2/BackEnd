package com.ll.quizzle.global.security.oauth2.dto;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class SecurityUser extends User implements OAuth2User {
    private final long id;
    private final String email;
    private final String provider;
    private final boolean isNewUser;
    private final Map<String, Object> attributes;
    private final String oauthId;

    // 일반 로그인용 생성자
    public SecurityUser(long id, String username, String email, String role) {
        super(username, "", List.of(new SimpleGrantedAuthority(role)));
        this.id = id;
        this.email = email;
        this.provider = null;
        this.isNewUser = false;
        this.attributes = null;
        this.oauthId = null;
    }

    // OAuth2 로그인용 생성자
    public SecurityUser(String email, String username, String provider,
                        boolean isNewUser, Map<String, Object> attributes,
                        Collection<? extends GrantedAuthority> authorities, String oauthId) {
        super(username, "", authorities);
        this.id = -1;
        this.email = email;
        this.provider = provider;
        this.isNewUser = isNewUser;
        this.attributes = attributes;
        this.oauthId = oauthId;
    }

    public static SecurityUser of(long id, String username, String email, String role) {
        return new SecurityUser(id, username, email, role);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return email;
    }

}
