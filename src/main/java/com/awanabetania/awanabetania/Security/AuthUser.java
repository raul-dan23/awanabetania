package com.awanabetania.awanabetania.Security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The authenticated caller, as established by {@link JwtAuthFilter} from a verified token
 * and a live database lookup.
 * <p>
 * Controllers use this instead of ids or roles sent in the request body or query string:
 * those are chosen by the client, while this is not. Child and leader ids come from two
 * separate tables and overlap, so an id alone never identifies a caller — always compare
 * it together with {@link #kind()}.
 *
 * @param kind {@code CHILD} or {@code LEADER}
 * @param id   database id within the table that {@code kind} names
 * @param role the leader's role as stored in the database; empty for children
 */
public record AuthUser(String kind, Integer id, String role) {

    public static final String CHILD = "CHILD";
    public static final String LEADER = "LEADER";

    public boolean isChild() {
        return CHILD.equals(kind);
    }

    public boolean isLeader() {
        return LEADER.equals(kind);
    }

    /** Directors and coordinators run the club and may manage other people's accounts. */
    public boolean isDirector() {
        return isLeader() && role != null
                && (role.equalsIgnoreCase("DIRECTOR") || role.equalsIgnoreCase("COORDONATOR"));
    }

    /** {@code true} when the caller is the account identified by {@code kind} and {@code id}. */
    public boolean is(String kind, Integer id) {
        return this.kind.equals(kind) && this.id.equals(id);
    }

    /**
     * Returns the caller of the current request, or {@code null} for anonymous requests
     * (which only reach the handful of public routes in {@link SecurityConfig}).
     */
    public static AuthUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user) return user;
        return null;
    }
}
