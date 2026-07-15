package com.chronos.chronos.dto;

import com.chronos.chronos.entity.User;

/** Safe user projection — never exposes the password hash. */
public record UserView(Long id, String name, String email, String role) {
    public static UserView of(User u) {
        return new UserView(u.getUserId(), u.getName(), u.getEmail(), u.getRole());
    }
}
