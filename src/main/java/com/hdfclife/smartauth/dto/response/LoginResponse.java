package com.hdfclife.smartauth.dto.response;

import java.util.Set;

public record LoginResponse(
        String message,
        String accessToken,
        String refreshToken,
        String username,
        Set<String> roles
) {}