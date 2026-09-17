package com.hdfclife.smartauth.model;

public record Claim(
        int claimId,
        String claimNumber,
        int policyId,
        String claimant,
        String status
) {
}