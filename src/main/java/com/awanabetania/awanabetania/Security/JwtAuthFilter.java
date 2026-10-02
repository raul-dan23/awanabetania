package com.awanabetania.awanabetania.Security;

import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
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
 * <p>
 * The account is looked up on every request, so a deleted account's token stops working
 * immediately and a leader's role comes from the database rather than from a token that
 * may predate a demotion.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ChildRepository childRepository;
    private final LeaderRepository leaderRepository;

    public JwtAuthFilter(JwtService jwtService,
                         ChildRepository childRepository,
                         LeaderRepository leaderRepository) {
        this.jwtService = jwtService;
        this.childRepository = childRepository;
        this.leaderRepository = leaderRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Claims claims = jwtService.parse(header.substring(7));
            AuthUser user = claims == null ? null : resolve(claims);
            if (user != null) {
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + user.kind()));
                if (!user.role().isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(
                            "ROLE_" + user.role().toUpperCase().replace(' ', '_')));
                }

                var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Maps verified claims to a live account.
     *
     * @return the caller, or {@code null} if the account no longer exists
     */
    private AuthUser resolve(Claims claims) {
        String kind = claims.get("kind", String.class);
        Integer id = claims.get("uid", Integer.class);
        if (kind == null || id == null) return null;

        if (AuthUser.CHILD.equals(kind)) {
            return childRepository.existsById(id) ? new AuthUser(AuthUser.CHILD, id, "") : null;
        }
        if (AuthUser.LEADER.equals(kind)) {
            return leaderRepository.findById(id)
                    .map(l -> new AuthUser(AuthUser.LEADER, id, l.getRole() == null ? "" : l.getRole()))
                    .orElse(null);
        }
        return null;
    }
}
