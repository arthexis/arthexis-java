package com.arthexis.platform.security.mfa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminMfaChallengeRepository extends JpaRepository<AdminMfaChallenge, Long> {

  Optional<AdminMfaChallenge> findByUsernameAndFactorTypeAndChallenge(
      String username, String factorType, String challenge);
}
