package com.microservices.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponse {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdAt;
    private String message;
}
