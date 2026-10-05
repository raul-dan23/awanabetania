package com.awanabetania.awanabetania.Dto;

import com.awanabetania.awanabetania.Model.Bon;
import com.awanabetania.awanabetania.Model.BonStatus;
import com.awanabetania.awanabetania.Model.Child;

/**
 * A receipt as the shop screen shows it, with the child's current balance so the cashier
 * sees at a glance whether it can be approved.
 */
public record BonResponse(
        Integer id,
        Integer childId,
        String childName,
        int childPoints,
        String leaderName,
        String items,
        Integer totalPoints,
        BonStatus status,
        String createdAt) {

    public static BonResponse from(Bon bon) {
        Child child = bon.getChild();
        return new BonResponse(
                bon.getId(),
                child.getId(),
                child.getName() + " " + child.getSurname(),
                child.getSeasonPoints() != null ? child.getSeasonPoints() : 0,
                bon.getLeaderName() != null ? bon.getLeaderName() : "",
                bon.getItems(),
                bon.getTotalPoints(),
                bon.getStatus(),
                bon.getCreatedAt() != null ? bon.getCreatedAt().toString() : "");
    }
}
