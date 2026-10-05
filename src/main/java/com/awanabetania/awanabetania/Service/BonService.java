package com.awanabetania.awanabetania.Service;

import com.awanabetania.awanabetania.Dto.BonApprovalResponse;
import com.awanabetania.awanabetania.Dto.BonResponse;
import com.awanabetania.awanabetania.Dto.CreateBonRequest;
import com.awanabetania.awanabetania.Exception.ApiException;
import com.awanabetania.awanabetania.Model.Bon;
import com.awanabetania.awanabetania.Model.BonStatus;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Repository.BonRepository;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fair receipts: a seller creates one, a cashier approves it (the child pays in season
 * points) or rejects it. Each receipt is approved or rejected exactly once, even when two
 * cashiers act on it at the same moment.
 */
@Service
@RequiredArgsConstructor
public class BonService {

    private final BonRepository bonRepository;
    private final ChildRepository childRepository;
    private final LeaderRepository leaderRepository;
    private final PointsService pointsService;
    private final SeasonService seasonService;

    /**
     * Creates a PENDING receipt. The seller recorded on it is the logged-in leader, not a
     * name sent by the client.
     *
     * @throws ApiException 404 for an unknown child
     */
    @Transactional
    public BonResponse create(CreateBonRequest request, AuthUser seller) {
        Child child = childRepository.findById(request.childId())
                .orElseThrow(() -> ApiException.notFound("Child not found."));

        Bon bon = new Bon();
        bon.setChild(child);
        bon.setLeaderName(sellerName(seller));
        bon.setItems(request.items());
        bon.setTotalPoints(request.totalPoints());
        bon.setSeasonId(seasonService.currentId());
        return BonResponse.from(bonRepository.save(bon));
    }

    /** The current season's pending receipts, newest first. */
    @Transactional(readOnly = true)
    public List<BonResponse> pending() {
        return bonRepository.findBySeasonIdAndStatusOrderByCreatedAtDesc(seasonService.currentId(), BonStatus.PENDING)
                .stream().map(BonResponse::from).toList();
    }

    /** The current season's receipts, newest first. */
    @Transactional(readOnly = true)
    public List<BonResponse> all() {
        return bonRepository.findBySeasonIdOrderByCreatedAtDesc(seasonService.currentId())
                .stream().map(BonResponse::from).toList();
    }

    /**
     * Approves a pending receipt and charges the child.
     * <p>
     * The receipt is first claimed (PENDING → APPROVED, which only one caller can win),
     * then the points are deducted. If the balance is too low, the exception rolls the
     * claim back and the receipt stays pending.
     *
     * @throws ApiException 404 unknown receipt; 409 not pending or not enough points
     */
    @Transactional
    public BonApprovalResponse approve(Integer id) {
        Bon bon = find(id);
        Child child = bon.getChild();
        if (bon.getTotalPoints() == null || bon.getTotalPoints() <= 0) {
            throw ApiException.conflict("Receipt has an invalid total and cannot be approved.");
        }

        if (bonRepository.markApproved(id, LocalDateTime.now()) == 0) {
            throw ApiException.conflict("Receipt is not pending.");
        }
        int remaining = pointsService.spend(child.getId(), bon.getTotalPoints());

        return new BonApprovalResponse("Receipt approved.", remaining,
                child.getName() + " " + child.getSurname());
    }

    /**
     * Rejects a pending receipt; the child's balance is untouched.
     *
     * @throws ApiException 404 unknown receipt; 409 not pending
     */
    @Transactional
    public void reject(Integer id) {
        find(id);
        if (bonRepository.markRejected(id) == 0) {
            throw ApiException.conflict("Receipt is not pending.");
        }
    }

    private Bon find(Integer id) {
        return bonRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Receipt not found."));
    }

    private String sellerName(AuthUser seller) {
        return leaderRepository.findById(seller.id())
                .map(l -> (l.getName() + " " + (l.getSurname() != null ? l.getSurname() : "")).trim())
                .orElseThrow(() -> ApiException.forbidden("Only leaders can create receipts."));
    }
}
