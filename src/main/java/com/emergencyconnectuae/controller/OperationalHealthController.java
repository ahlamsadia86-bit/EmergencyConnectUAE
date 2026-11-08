package com.emergencyconnectuae.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ops")
public class OperationalHealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;
    private boolean simulateRedisDown = false;

    public OperationalHealthController(DataSource dataSource,
                                       StringRedisTemplate redisTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/health-summary")
    public ResponseEntity<Map<String, Object>> healthSummary() {

        Map<String, Object> status = new HashMap<>();

        status.put("application", "UP");
        status.put("database", checkDatabase());
        status.put("redis", checkRedis());
        status.put("platform", "EmergencyConnectUAE");
        status.put("readiness", "READY_FOR_EMERGENCY_OPERATIONS");

        return ResponseEntity.ok(status);
    }

    private String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkRedis() {

        if (simulateRedisDown) {
            return "DOWN_SIMULATED";
        }

        try {
            String response = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();

            return "PONG".equalsIgnoreCase(response) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }
    
    @PostMapping("/simulate-redis-down")
    public ResponseEntity<String> simulateRedisDown() {
        simulateRedisDown = true;
        return ResponseEntity.ok("Simulated Redis downtime enabled");
    }

    @PostMapping("/simulate-redis-up")
    public ResponseEntity<String> simulateRedisUp() {
        simulateRedisDown = false;
        return ResponseEntity.ok("Simulated Redis downtime disabled");
    }
}