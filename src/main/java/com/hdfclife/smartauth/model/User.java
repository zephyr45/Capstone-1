package com.hdfclife.smartauth.model;

import java.util.Set;

public record User(
        String username,
        String password,
        Set<String> roles,
        boolean active)
{}
