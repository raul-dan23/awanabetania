package com.awanabetania.awanabetania.Service;

import com.awanabetania.awanabetania.Exception.ApiException;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Spending of children's season points: the fair's currency. */
@Service
@RequiredArgsConstructor
public class PointsService {

    private final ChildRepository childRepository;

    /**
     * Takes {@code amount} points from the child's season balance, or nothing at all.
     * The check and the deduction are one database statement, so concurrent purchases
     * cannot overdraw the balance. Called inside a larger transaction (approving a
     * receipt), a failure here rolls that work back too.
     *
     * @return the balance after the deduction
     * @throws ApiException 400 for a non-positive amount, 404 for an unknown child,
     *                      409 when the balance does not cover the amount
     */
    @Transactional
    public int spend(Integer childId, int amount) {
        if (amount <= 0) {
            throw ApiException.badRequest("Amount must be positive.");
        }
        if (childRepository.deductSeasonPoints(childId, amount) == 0) {
            int balance = balance(childId);
            throw ApiException.conflict("Insufficient points. Balance: " + balance + ", required: " + amount + ".");
        }
        return balance(childId);
    }

    /** @throws ApiException 404 for an unknown child */
    @Transactional(readOnly = true)
    public int balance(Integer childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> ApiException.notFound("Child not found."));
        return child.getSeasonPoints() != null ? child.getSeasonPoints() : 0;
    }
}
