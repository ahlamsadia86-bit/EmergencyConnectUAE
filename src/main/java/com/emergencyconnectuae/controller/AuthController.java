package com.emergencyconnectuae.controller;

import com.emergencyconnectuae.dto.*;
import com.emergencyconnectuae.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-mfa")
    public ResponseEntity<JwtResponse> verifyMfa(@Valid @RequestBody MfaRequest request) {
        return ResponseEntity.ok(authService.verifyMfa(request.getEmail(), request.getCode()));
    }
}