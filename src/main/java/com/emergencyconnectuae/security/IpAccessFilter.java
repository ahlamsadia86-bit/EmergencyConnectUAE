package com.emergencyconnectuae.security;

import com.emergencyconnectuae.dto.ErrorResponse;
import com.emergencyconnectuae.service.SecurityAnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

@Component
public class IpAccessFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final SecurityAnalyticsService securityAnalyticsService;

    private final Set<String> blockedIps = Set.of(
            "192.168.1.200"
    );

    public IpAccessFilter(ObjectMapper objectMapper,
            SecurityAnalyticsService securityAnalyticsService) {
this.objectMapper = objectMapper;
this.securityAnalyticsService = securityAnalyticsService;
}

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();

        if (blockedIps.contains(clientIp)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            securityAnalyticsService.record(
                    "BLOCKED_IP",
                    clientIp,
                    "UNKNOWN",
                    "Blocked IP attempted access"
            );

            ErrorResponse error = new ErrorResponse(
                    403,
                    "Forbidden",
                    "This IP address is blocked by security policy",
                    LocalDateTime.now()
            );

            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }

        filterChain.doFilter(request, response);
    }
}