package com.snehashis.docmind.service;

import com.snehashis.docmind.dto.RegisterUserRequest;
import com.snehashis.docmind.dto.UserDto;
import com.snehashis.docmind.entity.Role;
import com.snehashis.docmind.entity.User;
import com.snehashis.docmind.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDto registerUser(@Valid RegisterUserRequest registerUserRequest) {

        // Validation
        if (registerUserRequest.username().isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        if (userRepository.existsByUsername(registerUserRequest.username())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(registerUserRequest.username());
        user.setPassword(passwordEncoder.encode(registerUserRequest.password()));
        user.setEmail(registerUserRequest.email());
        user.setRole(Role.USER); // Set default role to USER

        User savedUser = userRepository.save(user);
        return new UserDto(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getRole());
    }
}
