package com.awanabetania.awanabetania.Dto;

import com.awanabetania.awanabetania.Model.Leader;

/**
 * A director in the dashboard's contact list. Only what the list shows: the endpoint is also
 * open to children's accounts, which anyone can create, so the rest of the leader record
 * (Google address, notes, rating) stays out.
 */
public record DirectorContactResponse(Integer id, String name, String surname, String role, String phoneNumber) {

    public static DirectorContactResponse from(Leader leader) {
        return new DirectorContactResponse(leader.getId(), leader.getName(), leader.getSurname(),
                leader.getRole(), leader.getPhoneNumber());
    }
}
