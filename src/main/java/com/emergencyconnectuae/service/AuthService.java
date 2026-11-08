package com.emergencyconnectuae.service;

import com.emergencyconnectuae.dto.*;
import com.emergencyconnectuae.model.Role;
import com.emergencyconnectuae.model.User;
import com.emergencyconnectuae.repository.UserRepository;
import com.emergencyconnectuae.security.JwtUtil;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
	private SecurityAnalyticsService securityAnalyticsService;
	private final StringRedisTemplate redisTemplate;
	

    public AuthService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            SecurityAnalyticsService securityAnalyticsService,StringRedisTemplate redisTemplate
            ) {
this.userRepository = userRepository;
this.passwordEncoder = passwordEncoder;
this.authenticationManager = authenticationManager;
this.jwtUtil = jwtUtil;
this.securityAnalyticsService = securityAnalyticsService;
this.redisTemplate = redisTemplate;
}

    public String register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Role role = request.getRole() == null ? Role.RESPONDER : request.getRole();

        boolean needsMfa = role == Role.DISPATCHER || role == Role.ADMIN;

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .mfaEnabled(needsMfa)
                .build();

        userRepository.save(user);

        return "User registered successfully as " + role;
    }

    public Object login(LoginRequest request) {

    	try {
    	    authenticationManager.authenticate(
    	            new UsernamePasswordAuthenticationToken(
    	                    request.getEmail(),
    	                    request.getPassword()
    	            )
    	    );
    	} catch (Exception e) {

    	    String failedLoginKey = "security:failed-logins:UNKNOWN";

    	    Long failed = redisTemplate.opsForValue().increment(failedLoginKey);

    	    if (failed != null && failed == 1) {
    	        redisTemplate.expire(failedLoginKey, Duration.ofMinutes(15));
    	    }

    	    securityAnalyticsService.record(
    	            "FAILED_LOGIN",
    	            "UNKNOWN",
    	            request.getEmail(),
    	            "Failed login attempt for email: " + request.getEmail()
    	    );

    	    throw new RuntimeException("Invalid email or password");
    	}

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isMfaEnabled()) {
            String code = generateMfaCode();

            user.setMfaCode(code);
            user.setMfaCodeExpiry(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);

            return "MFA required. Demo MFA code: " + code;
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return new JwtResponse(token, user.getRole().name());
    }

    public JwtResponse verifyMfa(String email, String code) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getMfaCode() == null || user.getMfaCodeExpiry() == null) {
            throw new RuntimeException("No MFA code generated");
        }

        if (LocalDateTime.now().isAfter(user.getMfaCodeExpiry())) {
            throw new RuntimeException("MFA code expired");
        }

        if (!user.getMfaCode().equals(code)) {
            throw new RuntimeException("Invalid MFA code");
        }

        user.setMfaCode(null);
        user.setMfaCodeExpiry(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return new JwtResponse(token, user.getRole().name());
    }

    private String generateMfaCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }
}
