package com.awanabetania.awanabetania.Security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the {@code Authorization: Bearer <token>} header on every request and, when the
 * token is valid, populates the Spring Security context with the caller's identity and
 * authorities.
 * <p>
 * Two authorities are granted per account: {@code ROLE_<KIND>} (CHILD or LEADER) and,
 * for leaders, {@code ROLE_<ROLE>} derived from the leader's role column — so a director
 * receives {@code ROLE_LEADER} and {@code ROLE_DIRECTOR}. Requests without a token pass
 * through unauthenticated; {@link SecurityConfig} then decides whether that is allowed.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Claims claims = jwtService.parse(header.substring(7));
            if (claims != null) {
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                String kind = claims.get("kind", String.class);
                if (kind != null && !kind.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + kind.toUpperCase()));
                }
                String role = claims.get("role", String.class);
                if (role != null && !role.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(
                            "ROLE_" + role.toUpperCase().replace(' ', '_')));
                }

                var auth = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}
