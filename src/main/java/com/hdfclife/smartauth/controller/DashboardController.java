package com.hdfclife.smartauth.controller;

import com.hdfclife.smartauth.dto.response.UserDashboardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class DashboardController {

    @GetMapping("/dashboard")
    public ResponseEntity<UserDashboardResponse> getDashboard() {

        return ResponseEntity.ok(
                new UserDashboardResponse(
                        2,
                        2,
                        "Active"
                )
        );
    }
}
