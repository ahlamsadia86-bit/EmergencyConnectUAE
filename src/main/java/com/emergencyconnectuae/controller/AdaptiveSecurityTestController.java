package com.emergencyconnectuae.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/adaptive-security/test")
public class AdaptiveSecurityTestController {

    private final StringRedisTemplate redisTemplate;

    public AdaptiveSecurityTestController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> blockIp(@RequestParam String ip) {

        redisTemplate.opsForValue().set(
                "security:temporary-block:" + ip,
                "BLOCKED",
                Duration.ofMinutes(5)
        );

        return ResponseEntity.ok("IP temporarily blocked for 5 minutes: " + ip);
    }

    @PostMapping("/suspicious")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> makeSuspicious(@RequestParam String ip) {

        redisTemplate.opsForValue().set("security:violations:" + ip, "5", Duration.ofMinutes(15));
        redisTemplate.opsForValue().set("security:failed-logins:" + ip, "3", Duration.ofMinutes(15));
        redisTemplate.opsForValue().set("security:access-denied:" + ip, "2", Duration.ofMinutes(15));

        return ResponseEntity.ok("Suspicious behavior simulated for IP: " + ip);
    }

    @PostMapping("/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> clearIp(@RequestParam String ip) {

        redisTemplate.delete("security:temporary-block:" + ip);
        redisTemplate.delete("security:violations:" + ip);
        redisTemplate.delete("security:failed-logins:" + ip);
        redisTemplate.delete("security:access-denied:" + ip);

        return ResponseEntity.ok("Adaptive security data cleared for IP: " + ip);
    }
}