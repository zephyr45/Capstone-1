package com.hdfc.jwtauth.services;

import com.hdfc.jwtauth.entity.User;
import com.hdfc.jwtauth.exceptions.DatabaseUnavailableException;
import com.hdfc.jwtauth.exceptions.DuplicateUsernameException;
import com.hdfc.jwtauth.exceptions.PasswordMismatchException;
import com.hdfc.jwtauth.repository.UserRepository;
import com.hdfc.jwtauth.web.RegisterRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

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

    @CircuitBreaker(
            name = "databaseCircuitBreakerCore",
            fallbackMethod = "databaseFallback"
    )
    public Optional<User> findUserForLogin(String username) {
        return userRepository.findByUsername(username);
    }

    private Optional<User> databaseFallback(
            String username,
            DataAccessException exception
    ) {
        throw databaseUnavailable(username, exception);
    }

    private Optional<User> databaseFallback(
            String username,
            TransactionException exception
    ) {
        throw databaseUnavailable(username, exception);
    }

    private Optional<User> databaseFallback(
            String username,
            PersistenceException exception
    ) {
        throw databaseUnavailable(username, exception);
    }

    private DatabaseUnavailableException databaseUnavailable(
            String username,
            RuntimeException cause
    ) {
        log.error("Login database lookup unavailable for username={}", username, cause);
        return new DatabaseUnavailableException(
                "Database is currently unavailable",
                cause
        );
    }
}
