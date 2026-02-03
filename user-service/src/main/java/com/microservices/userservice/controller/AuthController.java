package com.microservices.userservice.controller;

import com.microservices.userservice.dto.LoginRequest;
import com.microservices.userservice.dto.LoginResponse;
import com.microservices.userservice.dto.SignupRequest;
import com.microservices.userservice.dto.SignupResponse;
import com.microservices.userservice.model.User;
import com.microservices.userservice.security.JwtUtil;
import com.microservices.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication and token generation")
@Slf4j
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @PostMapping("/signup")
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns user details")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("Signup request received for username: {}", request.getUsername());
        SignupResponse response = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and generate JWT token", description = "Validates credentials and generates a JWT token for authentication")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for username: {}", request.getUsername());
        
        // Find user by username
        User user = userService.findByUsername(request.getUsername());
        
        // Validate password
        if (!userService.validatePassword(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password for username: {}", request.getUsername());
            throw new RuntimeException("Invalid username or password");
        }
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUsername(), "user-service");
        
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setExpiresIn(86400L); // 24 hours in seconds
        
        log.info("Login successful for username: {}", request.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/service-token")
    @Operation(summary = "Generate service token", description = "Generates a JWT token for service-to-service communication")
    public ResponseEntity<LoginResponse> generateServiceToken(@RequestParam String serviceName) {
        String token = jwtUtil.generateServiceToken(serviceName);
        
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setExpiresIn(86400L);
        
        return ResponseEntity.ok(response);
    }
}
