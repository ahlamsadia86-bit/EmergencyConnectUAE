package com.emergencyconnectuae.security;

import com.emergencyconnectuae.dto.ErrorResponse;
import com.emergencyconnectuae.service.SecurityAnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecurityAnalyticsService securityAnalyticsService;

    @Value("${app.security.rate-limit-per-minute:100}")
    private int baseLimitPerMinute;

    public RateLimitFilter(StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper,
                           SecurityAnalyticsService securityAnalyticsService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.securityAnalyticsService = securityAnalyticsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || path.startsWith("/css")
                || path.startsWith("/js")
                || path.startsWith("/images")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = request.getRemoteAddr();

        String requestKey = "rate-limit:requests:" + clientIp;
        String violationKey = "security:violations:" + clientIp;
        String failedLoginKey = "security:failed-logins:" + clientIp;
        String accessDeniedKey = "security:access-denied:" + clientIp;
        String blockKey = "security:temporary-block:" + clientIp;

        try {
            Boolean blocked = redisTemplate.hasKey(blockKey);

            if (Boolean.TRUE.equals(blocked)) {
                writeError(response, 403, "Forbidden",
                        "This IP is temporarily blocked due to suspicious behavior.");
                return;
            }

            int suspiciousScore = calculateSuspiciousScore(
                    violationKey,
                    failedLoginKey,
                    accessDeniedKey
            );

            int effectiveLimit = calculateAdaptiveLimit(suspiciousScore);

            Long count = redisTemplate.opsForValue().increment(requestKey);

            if (count != null && count == 1) {
                redisTemplate.expire(requestKey, Duration.ofMinutes(1));
            }

            if (count != null && count > effectiveLimit) {

                Long violations = redisTemplate.opsForValue().increment(violationKey);

                if (violations != null && violations == 1) {
                    redisTemplate.expire(violationKey, Duration.ofMinutes(15));
                }

                securityAnalyticsService.record(
                        "RATE_LIMIT_EXCEEDED",
                        clientIp,
                        "UNKNOWN",
                        "Adaptive limit exceeded. Suspicious score="
                                + suspiciousScore + ", effective limit=" + effectiveLimit
                );

                if (violations != null && violations >= 5) {
                    redisTemplate.opsForValue().set(
                            blockKey,
                            "BLOCKED",
                            Duration.ofMinutes(5)
                    );

                    securityAnalyticsService.record(
                            "TEMPORARY_IP_BLOCK",
                            clientIp,
                            "UNKNOWN",
                            "IP temporarily blocked after repeated rate-limit violations"
                    );

                    writeError(response, 403, "Forbidden",
                            "Too many suspicious requests. IP temporarily blocked for 5 minutes.");
                    return;
                }

                writeError(response, 429, "Too Many Requests",
                        "Adaptive rate limit exceeded. Current effective limit: "
                                + effectiveLimit + " requests/minute.");
                return;
            }

        } catch (Exception redisDown) {
            System.out.println("Redis unavailable in RateLimitFilter. Allowing request using graceful degradation.");
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int calculateSuspiciousScore(String violationKey,
                                         String failedLoginKey,
                                         String accessDeniedKey) {

        int score = 0;

        score += getRedisInt(violationKey) * 2;
        score += getRedisInt(failedLoginKey) * 3;
        score += getRedisInt(accessDeniedKey) * 2;

        return score;
    }

    private int calculateAdaptiveLimit(int suspiciousScore) {

        if (suspiciousScore >= 20) {
            return Math.max(5, baseLimitPerMinute / 4);
        }

        if (suspiciousScore >= 10) {
            return Math.max(10, baseLimitPerMinute / 2);
        }

        if (suspiciousScore >= 5) {
            return Math.max(20, (int) (baseLimitPerMinute * 0.75));
        }

        return baseLimitPerMinute;
    }

    private int getRedisInt(String key) {
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

    private void writeError(HttpServletResponse response,
                            int status,
                            String error,
                            String message) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");

        ErrorResponse errorResponse = new ErrorResponse(
                status,
                error,
                message,
                LocalDateTime.now()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}