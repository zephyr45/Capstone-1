package com.hdfclife.smartauth.model;


public record Policy(
        int policyId,
        String policyNumber,
        String policyHolder,
        String policyType,
        boolean active
) {
}