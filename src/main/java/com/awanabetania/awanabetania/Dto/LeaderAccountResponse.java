package com.awanabetania.awanabetania.Dto;

import com.awanabetania.awanabetania.Model.Leader;

/** A leader's login details as the Control Center shows them. */
public record LeaderAccountResponse(Integer id, String name, String surname, String username,
                                    String role, String email, boolean googleLinked) {

    public static LeaderAccountResponse from(Leader l) {
        return new LeaderAccountResponse(l.getId(), l.getName(), l.getSurname(), l.getUsername(),
                l.getRole(), l.getEmail(), l.isGoogleLinked());
    }
}
