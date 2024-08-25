package com.taskmanager.service;


import com.taskmanager.domain.User;
import com.taskmanager.domain.enums.Role;
import com.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User saveUser(User user) {

        if (user.getRoles().isEmpty()) {
            user.getRoles().add(Role.USER);
        }

        return userRepository.save(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
