package com.awanabetania.awanabetania.Dto;

/** Result of approving a receipt: the child's balance after the deduction. */
public record BonApprovalResponse(String message, int remainingPoints, String childName) {
}
