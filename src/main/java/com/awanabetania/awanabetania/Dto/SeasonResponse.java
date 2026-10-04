package com.awanabetania.awanabetania.Dto;

import com.awanabetania.awanabetania.Model.Season;
import com.awanabetania.awanabetania.Model.SeasonStatus;

import java.time.LocalDate;

/** A season as the Control Center and the dashboard show it. */
public record SeasonResponse(Integer id, String name, LocalDate startDate, LocalDate endDate, boolean active) {

    public static SeasonResponse from(Season s) {
        return new SeasonResponse(s.getId(), s.getName(), s.getStartDate(), s.getEndDate(),
                s.getStatus() == SeasonStatus.ACTIVE);
    }
}
