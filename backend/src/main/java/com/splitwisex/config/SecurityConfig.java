package com.splitwisex.config;

import org.springframework.security.config.Customizer;
import com.splitwisex.security.CustomAccessDeniedHandler;
import com.splitwisex.security.CustomAuthenticationEntryPoint;
import com.splitwisex.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Real security configuration for Stage 2.
 *
 * - Stateless: no HTTP sessions — every request must carry its own JWT.
 * - /api/auth/** and /api/health are public; everything else requires a
 *   valid bearer token, verified by JwtAuthenticationFilter.
 * - Auth failures return JSON (CustomAuthenticationEntryPoint /
 *   CustomAccessDeniedHandler) instead of Spring's default HTML/empty body.
 *
 * There is no group/expense endpoint to protect yet — those arrive in
 * Stage 3 — but wiring "authenticated() for everything else" now means
 * nothing needs to change here when they do.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // Preflight requests never carry the Authorization header,
                    // so they must be let through Security's filter chain
                    // unauthenticated — actual CORS headers are added by
                    // WebConfig's WebMvcConfigurer, further down the chain.
                    // Without this, once Stage 3 adds protected endpoints,
                    // browser preflight requests to them would be rejected
                    // here before ever reaching that CORS handling.
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/auth/**", "/api/health").permitAll()
                    // The WebSocket handshake itself carries no JWT (browsers
                    // can't attach an Authorization header to it) — auth for
                    // this endpoint happens one level up, at the STOMP CONNECT
                    // frame, via StompAuthChannelInterceptor.
                    .requestMatchers("/ws/**").permitAll()
                    .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
