package com.emergencyconnectuae.config;

import com.emergencyconnectuae.security.JwtAuthFilter;
import com.emergencyconnectuae.security.RateLimitFilter;
import com.emergencyconnectuae.security.CustomUserDetailsService;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.emergencyconnectuae.security.IpAccessFilter;
import com.emergencyconnectuae.security.CustomAccessDeniedHandler;
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final RateLimitFilter rateLimitFilter;
    private final IpAccessFilter ipAccessFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    
    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
            CustomUserDetailsService userDetailsService,
            RateLimitFilter rateLimitFilter,
            IpAccessFilter ipAccessFilter,
            CustomAccessDeniedHandler customAccessDeniedHandler) {
this.jwtAuthFilter = jwtAuthFilter;
this.userDetailsService = userDetailsService;
this.rateLimitFilter = rateLimitFilter;
this.ipAccessFilter = ipAccessFilter;
this.customAccessDeniedHandler = customAccessDeniedHandler;
}
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .headers(headers -> headers
                        .contentTypeOptions(content -> {})
                        .frameOptions(frame -> frame.sameOrigin())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; connect-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; img-src 'self' data:; font-src 'self' data: https://cdn.jsdelivr.net;"))
                )

                .authorizeHttpRequests(auth -> auth
                		.requestMatchers(
                		        "/",
                		        "/login",
                		        "/mfa",
                		        "/dashboard",
                		        "/incidents",
                		        "/resources",
                		        "/assignments",
                		        "/dispatch",
                		        "/reservations",
                		        "/audit-logs",
                		        "/register",
                		        "/operations",
                		        "/css/**",
                		        "/js/**",
                		        "/images/**",
                		        "/api/auth/**",
                		        "/swagger-ui.html",
                		        "/swagger-ui/**",
                		        "/v3/api-docs/**",
                		        "/actuator/health"
                		).permitAll()

                        .requestMatchers("/api/incidents/**").hasAnyRole("DISPATCHER", "RESPONDER", "ADMIN")
                        .requestMatchers("/api/resources/**").hasAnyRole("DISPATCHER", "HOSPITAL_ADMIN", "ADMIN")
                        .requestMatchers("/api/assignments/**").hasAnyRole("DISPATCHER", "ADMIN")
                        .requestMatchers("/api/reservations/**").hasAnyRole("HOSPITAL_ADMIN", "DISPATCHER", "ADMIN")
                        .requestMatchers("/api/audit-logs/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers("/api/dashboard/**").hasAnyRole("DISPATCHER", "RESPONDER", "HOSPITAL_ADMIN", "ADMIN")
                        .requestMatchers("/api/dispatch/**").hasAnyRole("DISPATCHER", "ADMIN")
                        .requestMatchers("/api/security-analytics/**").hasRole("ADMIN")
                        .requestMatchers("/api/ops/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers("/api/adaptive-security/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                                .authenticationProvider(authenticationProvider())

               
                .addFilterBefore(ipAccessFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();

    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}