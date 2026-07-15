package com.chronos.chronos.dto;

import com.chronos.chronos.entity.User;

public record AuthResponse(String token, long expiresIn, UserView user) {
    public static AuthResponse of(String token, long expiresIn, User u) {
        return new AuthResponse(token, expiresIn, UserView.of(u));
    }
}
