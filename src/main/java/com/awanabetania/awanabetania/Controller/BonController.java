package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Dto.BonApprovalResponse;
import com.awanabetania.awanabetania.Dto.BonResponse;
import com.awanabetania.awanabetania.Dto.CreateBonRequest;
import com.awanabetania.awanabetania.Dto.MessageResponse;
import com.awanabetania.awanabetania.Security.AuthUser;
import com.awanabetania.awanabetania.Service.BonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Purchase receipts (bons) for the end-of-season fair. A selling leader creates a receipt;
 * a cashier approves it after scanning the child's NFC card, which deducts the points, or
 * rejects it. Rules live in {@link BonService}; errors are problem responses (see
 * {@code GlobalExceptionHandler}).
 */
@RestController
@RequestMapping("/api/bons")
@RequiredArgsConstructor
public class BonController {

    private final BonService bonService;

    /** Creates a PENDING receipt; the seller is the logged-in leader. 400 invalid, 404 unknown child. */
    @PostMapping
    public BonResponse create(@Valid @RequestBody CreateBonRequest request) {
        return bonService.create(request, AuthUser.current());
    }

    /** Pending receipts, newest first. */
    @GetMapping("/pending")
    public List<BonResponse> pending() {
        return bonService.pending();
    }

    /** All receipts, newest first. */
    @GetMapping("/all")
    public List<BonResponse> all() {
        return bonService.all();
    }

    /** Approves and charges the child. 404 unknown; 409 not pending or not enough points. */
    @PostMapping("/{id}/approve")
    public BonApprovalResponse approve(@PathVariable Integer id) {
        return bonService.approve(id);
    }

    /** Rejects without charging. 404 unknown; 409 not pending. */
    @PostMapping("/{id}/reject")
    public MessageResponse reject(@PathVariable Integer id) {
        bonService.reject(id);
        return new MessageResponse("Receipt rejected.");
    }
}
