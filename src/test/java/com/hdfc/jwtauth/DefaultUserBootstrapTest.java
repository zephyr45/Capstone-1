package com.hdfc.jwtauth;

import com.hdfc.jwtauth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class DefaultUserBootstrapTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void seedsDefaultUsersOnStartup() {
        assertTrue(userRepository.existsByUsername("sachin"));
        assertTrue(userRepository.existsByUsername("admin"));
    }
}
