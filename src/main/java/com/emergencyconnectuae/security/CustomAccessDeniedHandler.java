package com.emergencyconnectuae.security;

import com.emergencyconnectuae.dto.ErrorResponse;
import com.emergencyconnectuae.service.SecurityAnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final SecurityAnalyticsService securityAnalyticsService;
    private final StringRedisTemplate redisTemplate;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper,
            SecurityAnalyticsService securityAnalyticsService,
            StringRedisTemplate redisTemplate) {
this.objectMapper = objectMapper;
this.securityAnalyticsService = securityAnalyticsService;
this.redisTemplate = redisTemplate;
}

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        String username = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName()
                : "UNKNOWN";

        String clientIp = request.getRemoteAddr();

        String accessDeniedKey = "security:access-denied:" + clientIp;

        Long deniedCount = redisTemplate.opsForValue().increment(accessDeniedKey);

        if (deniedCount != null && deniedCount == 1) {
            redisTemplate.expire(accessDeniedKey, Duration.ofMinutes(15));
        }
        securityAnalyticsService.record(
                "ACCESS_DENIED",
                clientIp,
                username,
                "Access denied for path: " + request.getRequestURI()
        );

        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "You do not have permission to access this resource",
                LocalDateTime.now()
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}