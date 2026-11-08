package com.emergencyconnectuae.controller;

import com.emergencyconnectuae.service.SecurityAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/security-analytics")
public class SecurityController {

    private final SecurityAnalyticsService securityAnalyticsService;

    public SecurityController(SecurityAnalyticsService securityAnalyticsService) {
        this.securityAnalyticsService = securityAnalyticsService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> summary() {
        return ResponseEntity.ok(securityAnalyticsService.summary());
    }
}