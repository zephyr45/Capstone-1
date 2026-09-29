package com.hdfc.jwtauth.services;

import com.hdfc.jwtauth.entity.User;
import com.hdfc.jwtauth.exceptions.DuplicateUsernameException;
import com.hdfc.jwtauth.exceptions.PasswordMismatchException;
import com.hdfc.jwtauth.repository.UserRepository;
import com.hdfc.jwtauth.web.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest request) {

        // 1. Confirm password
        if (!request.password().equals(request.confirmPassword())) {
            throw new PasswordMismatchException("Passwords do not match");
        }

        // 2. Check username
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException("Username already exists");
        }

        // 3. Hash password
        String encodedPassword =
                passwordEncoder.encode(request.password());

        // 4. Create USER
        User user = new User(
                request.username(),
                encodedPassword,
                "USER",
                true
        );

        // 5. Save to PostgreSQL
        return userRepository.save(user);
    }
}