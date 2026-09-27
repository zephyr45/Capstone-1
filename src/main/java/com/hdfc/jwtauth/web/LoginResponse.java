package com.hdfc.jwtauth.web;

import java.util.Set;

public record LoginResponse(
        String message,
        String username,
        Set<String> roles
) {

}