//package com.hdfc.jwtauth.services;
//
//import com.hdfc.jwtauth.entity.User;
//import com.hdfc.jwtauth.web.RegisterRequest;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//@Service
//public class InMemoryUserService {
//    private final PasswordEncoder passwordEncoder;
//
//    private final Map<String, User> users = new HashMap<>();
//
//    public InMemoryUserService(PasswordEncoder passwordEncoder) {
//        this.passwordEncoder = passwordEncoder;
//
//        users.put("sachin",
//                new User("sachin", "sachin123", Set.of("USER"), true));
//
//        users.put("admin",
//                new User("admin", "admin123", Set.of("USER", "ADMIN"), true));
//
//        users.put("rahul",
//                new User("rahul", "rahul123", Set.of("USER"), true));
//
//        users.put("sahil",
//                new User("sahil", "sahil123", Set.of("USER"), true));
//
//        users.put("rohit",
//                new User("rohit", "rohit123", Set.of("USER"), true));
//
//        users.put("amit",
//                new User("amit", "amit123", Set.of("USER"), true));
//
//        users.put("priya",
//                new User("priya", "priya123", Set.of("USER"), true));
//
//        users.put("neha",
//                new User("neha", "neha123", Set.of("USER"), true));
//
//        users.put("arjun",
//                new User("arjun", "arjun123", Set.of("USER"), false));
//
//        users.put("vikas",
//                new User("vikas", "vikas123", Set.of("USER"), false));
//    }
//
//    public User register(RegisterRequest request) {
//
//        // 1. Check password and confirm password
//        if (!request.password().equals(request.confirmPassword())) {
//            throw new IllegalArgumentException(
//                    "Passwords do not match"
//            );
//        }
//
//        // 2. Check if username already exists
//        if (users.containsKey(request.username())) {
//            throw new IllegalArgumentException(
//                    "Username already exists"
//            );
//        }
//
//        // 3. Hash password
//        String encodedPassword =
//                passwordEncoder.encode(request.password());
//
//        // 4. Create USER account
//        User user = new User(
//                request.username(),
//                encodedPassword,
//                Set.of("USER"),
//                true
//        );
//
//        // 5. Store user in memory
//        users.put(
//                request.username(),
//                user
//        );
//
//        // 6. Return created user
//        return user;
//    }
//    public User find(String username) {
//        return users.get(username);
//    }
//
//    public Collection<> getAllUsers() {
//        return users.values();
//    }
//}