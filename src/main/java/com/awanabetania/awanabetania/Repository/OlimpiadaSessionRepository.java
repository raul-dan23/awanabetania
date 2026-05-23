package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.OlimpiadaSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OlimpiadaSessionRepository extends JpaRepository<OlimpiadaSession, Integer> {
    Optional<OlimpiadaSession> findByCode(String code);
    List<OlimpiadaSession> findAllByOrderByCreatedAtDesc();
}
