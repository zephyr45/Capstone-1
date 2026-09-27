package com.hdfc.jwtauth.config;

import com.hdfc.jwtauth.entity.User;
import com.hdfc.jwtauth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Configuration
class DataInitializer {

    @Bean
    CommandLineRunner createDefaultUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            if (!userRepository.existsByUsername("admin")) {

                User admin = new User(
                        "admin",
                        passwordEncoder.encode("Admin@123"),
                        "ADMIN",
                        true
                );

                userRepository.save(admin);

                log.info("Default admin user successfully initialized.");

            } else {
                log.info("Admin user already exists. Skipping initialization.");
            }

            if (!userRepository.existsByUsername("sachin")) {

                User sachin = new User(
                        "sachin",
                        passwordEncoder.encode("password"),
                        "USER",
                        true
                );

                userRepository.save(sachin);

                log.info("Default user sachin successfully initialized.");

            } else {
                log.info("User sachin already exists. Skipping initialization.");
            }
        };
    }
}