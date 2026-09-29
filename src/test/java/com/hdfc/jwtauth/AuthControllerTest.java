package com.hdfc.jwtauth;

import com.hdfc.jwtauth.entity.User;
import com.hdfc.jwtauth.repository.UserRepository;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // Rolls back DB operations after each test execution
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // Ensures clean database state before every test
        clearRateLimiters();
    }

    private void clearRateLimiters() {
        rateLimiterRegistry.getAllRateLimiters().stream()
                .map(RateLimiter::getName)
                .toList()
                .forEach(rateLimiterRegistry::remove);
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "testuser",
                                    "password": "Test@123",
                                    "confirmPassword": "Test@123"
                                }
                                """))
                .andExpect(status().isCreated());

        User user = userRepository
                .findByUsername("testuser")
                .orElseThrow();

        assertEquals("testuser", user.getUsername());
        assertEquals("USER", user.getRole());
        assertTrue(user.isActive());

        assertTrue(
                passwordEncoder.matches(
                        "Test@123",
                        user.getPassword()
                )
        );
    }

    @Test
    void shouldRejectDuplicateUsername() throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "duplicateuser",
                                "password": "Test@123",
                                "confirmPassword": "Test@123"
                            }
                            """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "duplicateuser",
                                "password": "Test@123",
                                "confirmPassword": "Test@123"
                            }
                            """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectPasswordMismatch() throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "mismatchuser",
                                "password": "Test@123",
                                "confirmPassword": "Different@123"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidRegistrationData() throws Exception {

        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "",
                                "password": "",
                                "confirmPassword": ""
                            }
                            """))
                .andReturn();

        int actualStatus = result.getResponse().getStatus();
        String actualBody = result.getResponse().getContentAsString();

        System.out.println("ACTUAL STATUS = " + actualStatus);
        System.out.println("ACTUAL BODY = " + actualBody);

        assertEquals(400, actualStatus,
                "Expected 400 but got " + actualStatus +
                        ". Response body: " + actualBody);
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {

        User user = new User(
                "loginuser",
                passwordEncoder.encode("Test@123"),
                "USER",
                true
        );

        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "username": "loginuser",
                        "password": "Test@123"
                    }
                    """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectLoginWithWrongPassword() throws Exception {

        User user = new User(
                "wrongpassworduser",
                passwordEncoder.encode("Correct@123"),
                "USER",
                true
        );

        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "username": "wrongpassworduser",
                        "password": "Wrong@123"
                    }
                    """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotLockAccountAfterFiveWrongPasswords() throws Exception {

        User user = new User(
                "no-lockout-user",
                passwordEncoder.encode("Correct@123"),
                "USER",
                true
        );

        userRepository.save(user);

        for (int attempt = 0; attempt < 6; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                        {
                            "username": "no-lockout-user",
                            "password": "Wrong@123"
                        }
                        """))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void shouldRejectLoginWithUnknownUsername() throws Exception {

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "username": "doesnotexist",
                        "password": "Test@123"
                    }
                    """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLoginForInactiveUser() throws Exception {

        User user = new User(
                "inactiveuser",
                passwordEncoder.encode("Test@123"),
                "USER",
                false
        );

        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "username": "inactiveuser",
                        "password": "Test@123"
                    }
                    """))
                .andExpect(status().isForbidden());
    }
}
