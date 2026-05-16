package com.payup.userservice.auth.oauth2;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User delegate;
    private final Long userId;
    private final String role;

    public CustomOAuth2User(OAuth2User delegate, Long userId, String role) {
        this.delegate = delegate;
        this.userId = userId;
        this.role = role;
    }

    @Override
    public Map<String, Object> getAttributes() { return delegate.getAttributes(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getName() { return delegate.getName(); }

    public Long getUserId() { return userId; }

    public String getRole() { return role; }
}
