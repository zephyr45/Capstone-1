package com.hdfclife.smartauth.dto.response;

public record UserDashboardResponse(
        int policies,
        int claims,
        String profile
) {}