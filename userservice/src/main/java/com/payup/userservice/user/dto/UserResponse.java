package com.payup.userservice.user.dto;

import com.payup.userservice.user.User;

public record UserResponse(Long id, String email, String name, String provider, String role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getProvider(), user.getRole());
    }
}
