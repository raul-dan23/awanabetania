package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Dto.NewSeasonPreviewResponse;
import com.awanabetania.awanabetania.Dto.RenameSeasonRequest;
import com.awanabetania.awanabetania.Dto.SeasonResponse;
import com.awanabetania.awanabetania.Dto.StartSeasonRequest;
import com.awanabetania.awanabetania.Security.AdminPinVerifier;
import com.awanabetania.awanabetania.Security.AuthUser;
import com.awanabetania.awanabetania.Service.SeasonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Starting a new season and renaming seasons, from the Control Center. Like every
 * {@code /api/admin} route: a director or coordinator login plus the admin PIN (403).
 */
@RestController
@RequestMapping("/api/admin/seasons")
@RequiredArgsConstructor
public class SeasonAdminController {

    private final SeasonService seasonService;
    private final AdminPinVerifier pinVerifier;

    /** What a new season would reset, and whether an open meeting prevents it now. */
    @GetMapping("/preview")
    public NewSeasonPreviewResponse preview(@RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        pinVerifier.verify(pin);
        return seasonService.preview();
    }

    /**
     * Closes the current season, archives it and starts a new one. 400 invalid name,
     * 403 PIN, 409 the season changed meanwhile, the name is taken or a meeting is open.
     */
    @PostMapping
    public SeasonResponse start(@RequestHeader(value = "X-Admin-Pin", required = false) String pin,
                                @Valid @RequestBody StartSeasonRequest request) {
        pinVerifier.verify(pin);
        return seasonService.startNew(request.currentSeasonId(), request.name(), AuthUser.current());
    }

    /** 400 invalid name, 403 PIN, 404 unknown season, 409 name taken. */
    @PutMapping("/{id}")
    public SeasonResponse rename(@PathVariable Integer id,
                                 @RequestHeader(value = "X-Admin-Pin", required = false) String pin,
                                 @Valid @RequestBody RenameSeasonRequest request) {
        pinVerifier.verify(pin);
        return seasonService.rename(id, request.name());
    }
}
