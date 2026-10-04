package com.awanabetania.awanabetania.Dto;

import java.time.LocalDate;

/**
 * What starting a new season would change, shown to the director before confirming.
 *
 * @param openMeetingDate a meeting that is not closed yet but already has scores: the new
 *                        season cannot start until it is closed. {@code null} when there is none
 */
public record NewSeasonPreviewResponse(SeasonResponse current,
                                       long children,
                                       long childrenWithPoints,
                                       long pointsBalance,
                                       long scores,
                                       long evaluations,
                                       long suspensions,
                                       long pendingBons,
                                       long plannedMeetings,
                                       LocalDate openMeetingDate) {
}
