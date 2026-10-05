package com.awanabetania.awanabetania.Service;

import com.awanabetania.awanabetania.Exception.ApiException;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Security.AuthUser;
import com.awanabetania.awanabetania.Security.GoogleIdTokenVerifier;
import com.awanabetania.awanabetania.Security.GoogleIdentity;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * "Continue with Google" for leaders. Only leaders the director has added can get in:
 * Google proves who the person is, and the leader list decides whether they have access.
 */
@Service
@RequiredArgsConstructor
public class GoogleSignInService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSignInService.class);

    private final GoogleIdTokenVerifier verifier;
    private final LeaderRepository leaderRepository;

    /**
     * The leader behind a Google sign-in. The first sign-in finds the leader by the invited
     * address and binds Google's permanent account id; later ones find them by that id.
     *
     * @throws ApiException 403 when the account is not on the leader list or the address
     *                      is already bound to a different Google account
     */
    @Transactional
    public Leader signIn(String credential) {
        GoogleIdentity google = verifier.verify(credential);

        Optional<Leader> bound = leaderRepository.findByGoogleSub(google.subject());
        if (bound.isPresent()) return bound.get();

        Optional<Leader> invited = leaderRepository.findByEmailIgnoreCase(google.email());
        if (invited.isEmpty()) {
            // Logged so the director can see which address to add when a leader cannot get in
            log.info("Google sign-in refused: {} is not on the leader list", google.email());
            throw ApiException.forbidden("The Google account " + google.email()
                    + " has no access. Ask the director to add it in the Control Center.");
        }
        Leader leader = invited.get();
        if (leader.getGoogleSub() != null) {
            log.warn("Google sign-in refused: {} is linked to a different Google account", google.email());
            throw ApiException.forbidden("This address is linked to a different Google account.");
        }
        leader.setGoogleSub(google.subject());
        return leaderRepository.save(leader);
    }

    /**
     * Binds the caller's Google account to their leader account, from "Contul Meu", so an
     * existing leader can switch to Google without the director typing their address.
     *
     * @throws ApiException 409 when that Google account or address belongs to another leader
     */
    @Transactional
    public Leader link(AuthUser me, String credential) {
        GoogleIdentity google = verifier.verify(credential);
        Leader leader = leaderRepository.findById(me.id()).orElseThrow();

        leaderRepository.findByGoogleSub(google.subject())
                .filter(other -> !other.getId().equals(leader.getId()))
                .ifPresent(other -> { throw ApiException.conflict("This Google account is already linked to another leader."); });
        leaderRepository.findByEmailIgnoreCase(google.email())
                .filter(other -> !other.getId().equals(leader.getId()))
                .ifPresent(other -> { throw ApiException.conflict("The address " + google.email() + " belongs to another leader."); });

        leader.setEmail(google.email());
        leader.setGoogleSub(google.subject());
        return leaderRepository.save(leader);
    }
}
