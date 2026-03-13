package com.irene.irene.auth.dto;

import com.irene.irene.auth.model.Role;

public record AuthResponse(
        Long userId,
        String email,
        Role role,
        String token
) {
}
