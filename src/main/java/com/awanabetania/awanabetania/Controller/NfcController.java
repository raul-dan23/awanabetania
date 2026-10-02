package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Dto.CardAssignedResponse;
import com.awanabetania.awanabetania.Dto.CardAssignmentRequest;
import com.awanabetania.awanabetania.Dto.CardHolderResponse;
import com.awanabetania.awanabetania.Dto.SpendRequest;
import com.awanabetania.awanabetania.Dto.SpendResponse;
import com.awanabetania.awanabetania.Exception.ApiException;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Security.SharedSecrets;
import com.awanabetania.awanabetania.Service.CardService;
import com.awanabetania.awanabetania.Service.PointsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * Card operations for a device holding the shared {@code X-NFC-Token} instead of a login.
 * These routes are public in {@code SecurityConfig}; the token is checked here.
 */
@RestController
@RequestMapping("/api/nfc")
public class NfcController {

    private final CardService cardService;
    private final PointsService pointsService;
    private final String nfcToken;

    public NfcController(CardService cardService, PointsService pointsService,
                         @Value("${nfc.token}") String nfcToken) {
        this.cardService = cardService;
        this.pointsService = pointsService;
        this.nfcToken = nfcToken;
    }

    /** Binds a card to a child, taking it off any previous owner. 403 bad token, 404 unknown child. */
    @PostMapping("/register")
    public CardAssignedResponse register(@RequestHeader(value = "X-NFC-Token", required = false) String token,
                                         @Valid @RequestBody CardAssignmentRequest request) {
        checkToken(token);
        Child child = cardService.assign(request.childId(), request.uid());
        return new CardAssignedResponse("Card registered.", child.getId(),
                child.getName() + " " + child.getSurname(), child.getNfcUid());
    }

    /** The card holder and their balance. 403 bad token, 404 unknown card. */
    @GetMapping("/{uid}")
    public CardHolderResponse holder(@PathVariable String uid,
                                     @RequestHeader(value = "X-NFC-Token", required = false) String token) {
        checkToken(token);
        Child child = cardService.holder(uid);
        return new CardHolderResponse(child.getId(), child.getName(), child.getSurname(),
                child.getSeasonPoints() != null ? child.getSeasonPoints() : 0, uid);
    }

    /** Spends points with the card. 400 bad amount, 403 bad token, 404 unknown card, 409 balance too low. */
    @PostMapping("/{uid}/spend")
    public SpendResponse spend(@PathVariable String uid,
                               @RequestHeader(value = "X-NFC-Token", required = false) String token,
                               @Valid @RequestBody SpendRequest request) {
        checkToken(token);
        Child child = cardService.holder(uid);
        int remaining = pointsService.spend(child.getId(), request.amount());
        return new SpendResponse("Transaction successful.", request.amount(), remaining,
                child.getName() + " " + child.getSurname());
    }

    private void checkToken(String token) {
        if (!SharedSecrets.matches(nfcToken, token)) {
            throw ApiException.forbidden("Invalid NFC token.");
        }
    }
}
