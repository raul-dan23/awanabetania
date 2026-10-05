package com.awanabetania.awanabetania.Dto;

import java.util.List;

/** The record of a closed season: the children's final standings and the leaders' ratings. */
public record SeasonResultsResponse(SeasonResponse season,
                                    long meetings,
                                    List<ChildResult> children,
                                    List<LeaderResult> leaders) {

    /**
     * One child's season. Ranked by points earned at meetings; {@code pointsLeft} is the
     * balance after the fair ({@code earnedPoints - spentPoints}, plus any manual changes).
     */
    public record ChildResult(int rank, Integer id, String name, String surname,
                              int earnedPoints, int spentPoints, int pointsLeft,
                              int attendance, int streak, int lessons, int badges,
                              boolean manual, boolean shirt, boolean hat, int warnings) {
    }

    /** A leader's average rating that season, from the director's evaluations. */
    public record LeaderResult(Integer id, String name, String surname, double rating, long evaluations) {
    }
}
