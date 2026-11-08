package com.emergencyconnectuae.service;

import com.emergencyconnectuae.model.SecurityEvent;
import com.emergencyconnectuae.repository.SecurityEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SecurityAnalyticsService {

    private final SecurityEventRepository securityEventRepository;

    public SecurityAnalyticsService(SecurityEventRepository securityEventRepository) {
        this.securityEventRepository = securityEventRepository;
    }

    public void record(String eventType, String ipAddress, String username, String details) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(eventType)
                .ipAddress(ipAddress)
                .username(username)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();

        securityEventRepository.save(event);
    }

    public Map<String, Object> summary() {
        Map<String, Object> map = new HashMap<>();

        map.put("totalSecurityEvents", securityEventRepository.count());
        map.put("rateLimitViolations", securityEventRepository.countByEventType("RATE_LIMIT_EXCEEDED"));
        map.put("blockedIpAttempts", securityEventRepository.countByEventType("BLOCKED_IP"));
        map.put("accessDeniedEvents", securityEventRepository.countByEventType("ACCESS_DENIED"));
        map.put("failedLoginEvents", securityEventRepository.countByEventType("FAILED_LOGIN"));

        return map;
    }
}