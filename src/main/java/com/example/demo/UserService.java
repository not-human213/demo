package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.example.demo.User;

@Service
public class UserService {

    private final List<User> users = new ArrayList<>();

    // Method to save a user
    public void saveUser(User user) {
        users.add(user);
    }

    // Method to find a user by username
    public User findByUsername(String username) {
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    // Method to retrieve all users
    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    // Method to delete a user by username
    public void deleteUser(String username) {
        users.removeIf(user -> user.getUsername().equals(username));
    }
} 