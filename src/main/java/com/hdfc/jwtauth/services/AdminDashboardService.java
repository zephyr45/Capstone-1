package com.hdfc.jwtauth.services;

import com.hdfc.jwtauth.repository.UserRepository;
import com.hdfc.jwtauth.web.AdminDashboardResponse;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final InMemoryPolicyService policyService;
    private final InMemoryClaimService claimService;

    public AdminDashboardService(
            UserRepository userRepository,
            InMemoryPolicyService policyService,
            InMemoryClaimService claimService
    ) {
        this.userRepository = userRepository;
        this.policyService = policyService;
        this.claimService = claimService;
    }

    public AdminDashboardResponse getDashboard() {

        int totalUsers = (int) userRepository.count();

        int activeUsers = (int) userRepository.countByActiveTrue();

        int totalPolicies = (int) policyService.getTotalPolicies();

        int activePolicies = (int) policyService.getActivePolicies();

        int totalClaims = (int) claimService.getTotalClaims();

        int pendingClaims = (int) claimService.getPendingClaims();

        return new AdminDashboardResponse(
                "Welcome, Admin",
                totalUsers,
                activeUsers,
                totalPolicies,
                activePolicies,
                totalClaims,
                pendingClaims
        );
    }
}

