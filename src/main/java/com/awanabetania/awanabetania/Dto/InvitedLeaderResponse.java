package com.awanabetania.awanabetania.Dto;

import com.awanabetania.awanabetania.Model.Leader;

/**
 * A leader the director just added. {@code temporaryPassword} is set when the leader cannot
 * use Google (no address given, or Google sign-in not configured): it is shown once, and the
 * leader picks their own password at the first login. Otherwise it is {@code null}.
 */
public record InvitedLeaderResponse(Integer id, String name, String surname, String username,
                                    String role, String email, boolean googleLinked,
                                    String temporaryPassword) {

    public static InvitedLeaderResponse from(Leader l, String temporaryPassword) {
        return new InvitedLeaderResponse(l.getId(), l.getName(), l.getSurname(), l.getUsername(),
                l.getRole(), l.getEmail(), l.isGoogleLinked(), temporaryPassword);
    }
}
