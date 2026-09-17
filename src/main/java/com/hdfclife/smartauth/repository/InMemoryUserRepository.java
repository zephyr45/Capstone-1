package com.hdfclife.smartauth.repository;

import com.hdfclife.smartauth.model.User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InMemoryUserRepository {

    private final Map<String, User> users = new HashMap<>();

    public InMemoryUserRepository() {

        users.put("user",
                new User("user", "password", Set.of("USER"), true));

        users.put("sachin",
                new User("sachin", "sachin123", Set.of("USER"), true));

        users.put("admin",
                new User("admin", "admin123", Set.of("USER", "ADMIN"), true));

        users.put("rahul",
                new User("rahul", "rahul123", Set.of("USER"), true));

        users.put("sahil",
                new User("sahil", "sahil123", Set.of("USER"), true));

        users.put("rohit",
                new User("rohit", "rohit123", Set.of("USER"), true));

        users.put("amit",
                new User("amit", "amit123", Set.of("USER"), true));

        users.put("priya",
                new User("priya", "priya123", Set.of("USER"), true));

        users.put("neha",
                new User("neha", "neha123", Set.of("USER"), true));

        users.put("arjun",
                new User("arjun", "arjun123", Set.of("USER"), false));

        users.put("vikas",
                new User("vikas", "vikas123", Set.of("USER"), false));
    }
    public User find(String username) {
        return users.get(username);
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }
}
