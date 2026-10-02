package com.awanabetania.awanabetania.Service;

import com.awanabetania.awanabetania.Exception.ApiException;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NFC cards. A card stores nothing but its hardware UID; this service maps UIDs to children.
 * Used by both the Control Center and the NFC bridge endpoints.
 */
@Service
@RequiredArgsConstructor
public class CardService {

    private final ChildRepository childRepository;

    /**
     * Binds the card to the child. A UID belongs to one child at most, so the card is first
     * taken off any other child that had it.
     *
     * @return the card's new owner
     * @throws ApiException 404 for an unknown child
     */
    @Transactional
    public Child assign(Integer childId, String uid) {
        String cardUid = uid.trim();
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> ApiException.notFound("Child not found."));

        childRepository.findByNfcUid(cardUid)
                .filter(previous -> !previous.getId().equals(childId))
                .ifPresent(previous -> {
                    previous.setNfcUid(null);
                    // Written before the new binding: the column is unique.
                    childRepository.saveAndFlush(previous);
                });

        child.setNfcUid(cardUid);
        return childRepository.save(child);
    }

    /** @throws ApiException 404 for an unknown child */
    @Transactional
    public void remove(Integer childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> ApiException.notFound("Child not found."));
        child.setNfcUid(null);
        childRepository.save(child);
    }

    /** @throws ApiException 404 when no child has this card */
    @Transactional(readOnly = true)
    public Child holder(String uid) {
        return childRepository.findByNfcUid(uid)
                .orElseThrow(() -> ApiException.notFound("Unknown card."));
    }
}
