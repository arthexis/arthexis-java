package com.arthexis.platform.security;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminStepUpSessionRepository extends JpaRepository<AdminStepUpSession, String> {

  Optional<AdminStepUpSession> findByTokenAndUsername(String token, String username);

  long deleteByExpiresAtBefore(Instant cutoff);
}
