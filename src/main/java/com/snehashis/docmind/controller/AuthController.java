package com.snehashis.docmind.controller;

import com.snehashis.docmind.dto.LoginRequest;
import com.snehashis.docmind.dto.LoginResponse;
import com.snehashis.docmind.dto.RegisterUserRequest;
import com.snehashis.docmind.dto.UserDto;
import com.snehashis.docmind.entity.User;
import com.snehashis.docmind.repository.UserRepository;
import com.snehashis.docmind.service.CustomUserDetail;
import com.snehashis.docmind.service.JwtService;
import com.snehashis.docmind.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest loginRequest
    ){
        // TODO
        // authenticate and return the token and user details
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                loginRequest.username(),
                loginRequest.password()
        );
        Authentication authenticated = authenticationManager.authenticate(authentication);

        User user = userRepository.findByUsername(loginRequest.username()).orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(new CustomUserDetail(user));

        var response = new LoginResponse(token, new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(
           @Valid @RequestBody RegisterUserRequest registerUserRequest
    ) {
        UserDto userDto = userService.registerUser(registerUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }
}
