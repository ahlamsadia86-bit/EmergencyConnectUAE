package com.emergencyconnectuae.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/adaptive-security")
public class AdaptiveSecurityController {

    private final StringRedisTemplate redisTemplate;

    public AdaptiveSecurityController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> status(@RequestParam String ip) {

        Map<String, Object> map = new HashMap<>();

        String violationKey = "security:violations:" + ip;
        String failedLoginKey = "security:failed-logins:" + ip;
        String accessDeniedKey = "security:access-denied:" + ip;
        String blockKey = "security:temporary-block:" + ip;

        int violations = getInt(violationKey);
        int failedLogins = getInt(failedLoginKey);
        int accessDenied = getInt(accessDeniedKey);

        int suspiciousScore =
                (violations * 2)
                        + (failedLogins * 3)
                        + (accessDenied * 2);

        map.put("ip", ip);
        map.put("rateLimitViolations", violations);
        map.put("failedLogins", failedLogins);
        map.put("accessDeniedEvents", accessDenied);
        map.put("suspiciousScore", suspiciousScore);
        map.put("temporarilyBlocked", Boolean.TRUE.equals(redisTemplate.hasKey(blockKey)));

        if (suspiciousScore >= 20) {
            map.put("riskLevel", "HIGH");
            map.put("adaptiveAction", "STRICT_LIMIT_OR_TEMPORARY_BLOCK");
        } else if (suspiciousScore >= 10) {
            map.put("riskLevel", "MEDIUM");
            map.put("adaptiveAction", "LIMIT_REDUCED_BY_50_PERCENT");
        } else if (suspiciousScore >= 5) {
            map.put("riskLevel", "LOW");
            map.put("adaptiveAction", "LIMIT_REDUCED_BY_25_PERCENT");
        } else {
            map.put("riskLevel", "NORMAL");
            map.put("adaptiveAction", "BASE_LIMIT");
        }

        return ResponseEntity.ok(map);
    }

    private int getInt(String key) {
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
}