package com.awanabetania.awanabetania.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Central security policy for the API.
 * <p>
 * The application is a stateless JSON API behind a React SPA, so sessions and CSRF
 * tokens are disabled and every call authenticates with a JWT instead. Anything not
 * explicitly listed as public requires a valid token — the default is deny, so a new
 * controller added later is protected automatically rather than silently exposed.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    /** Comma-separated list of browser origins allowed to call the API. */
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * BCrypt replaces the previous reversible AES encryption of passwords.
     * Unlike AES, a BCrypt hash cannot be turned back into the original password,
     * so a database leak no longer exposes anyone's credentials.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            // No cookies are used, so there is no CSRF surface to protect.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Answer 401 (not Spring's default 403) when a token is missing or invalid,
            // so the browser client can tell "log in again" apart from "not allowed".
            .exceptionHandling(e -> e.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                // --- SPA shell and static assets ---
                .requestMatchers(HttpMethod.GET,
                        "/", "/index.html", "/assets/**", "/favicon.ico",
                        "/*.png", "/*.svg", "/*.ico", "/*.webmanifest").permitAll()
                .requestMatchers(HttpMethod.GET, "/{path:[^\\.]*}").permitAll()

                // --- Monitoring: status and deployed version only (see application.properties) ---
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()

                // --- Authentication ---
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register", "/api/auth/google").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/config").permitAll()

                // --- Olimpiada: guest arbiters score without an account ---
                .requestMatchers(HttpMethod.GET,
                        "/api/olimpiada/session/*",
                        "/api/olimpiada/session/*/round-status",
                        "/api/olimpiada/session/*/compare").permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/olimpiada/session/*/score",
                        "/api/olimpiada/session/*/extra").permitAll()

                // --- NFC bridge: guarded by its own X-NFC-Token shared secret ---
                .requestMatchers("/api/nfc/**").permitAll()

                // --- Children: their own profile only (ownership checked in the controllers) ---
                // Anyone can register a child account without a code, so ROLE_CHILD must
                // never reach the club's management endpoints.
                .requestMatchers(HttpMethod.GET, "/api/stickers", "/api/dashboard/stats").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/children/*").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/children/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/children/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/account/request-deletion", "/api/account/password").authenticated()

                // --- Control Center: directors and coordinators (plus the admin PIN) ---
                .requestMatchers("/api/admin/**").hasAnyRole("DIRECTOR", "COORDONATOR")

                // --- Everything else: leaders only ---
                .anyRequest().hasRole("LEADER")
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Restricts cross-origin calls to the configured origins. This replaces the
     * {@code @CrossOrigin(origins = "*")} annotations that previously allowed any
     * website on the internet to call the API with a victim's browser.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type",
                "X-Admin-Pin", "X-NFC-Token"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
