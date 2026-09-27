package com.hdfc.jwtauth.userrole;

import com.hdfc.jwtauth.entity.User;
import com.hdfc.jwtauth.security.TokenStore;
import com.hdfc.jwtauth.services.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleBasedTestingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenStore tokenStore;

    @Test
    void shouldRejectUnauthenticatedUserFromProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/user/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowUserRoleToAccessUserDashboard() throws Exception {
        mockMvc.perform(get("/user/dashboard")
                        .cookie(accessTokenFor("user", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("Active"));
    }

    @Test
    void shouldAllowAdminRoleToAccessUserDashboard() throws Exception {
        mockMvc.perform(get("/user/dashboard")
                        .cookie(accessTokenFor("admin", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("Active"));
    }

    @Test
    void shouldDenyUserRoleFromAccessingAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .cookie(accessTokenFor("user", "USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("You do not have permission to access this resource"));
    }

    @Test
    void shouldAllowAdminRoleToAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .cookie(accessTokenFor("admin", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.welcomeMessage").value("Welcome, Admin"));
    }

    private Cookie accessTokenFor(String username, String role) {
        String token = jwtService.generateAccessToken(
                new User(username, "unused-password", role, true));
        tokenStore.save(token, username);
        return new Cookie("ACCESS_TOKEN", token);
    }
}
