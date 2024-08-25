package com.taskmanager.repository;

import com.taskmanager.domain.JwpToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JwpTokenRepository extends JpaRepository<JwpToken, Long> {
    Optional<JwpToken> findByToken(String token);
}
