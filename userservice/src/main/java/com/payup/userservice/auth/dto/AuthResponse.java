package com.payup.userservice.auth.dto;

public record AuthResponse(
    String token,
    String email,
    String name,
    String role
) {}
