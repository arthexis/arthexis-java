package com.arthexis.platform.security.mfa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminTotpEnrollmentRepository extends JpaRepository<AdminTotpEnrollment, Long> {

  Optional<AdminTotpEnrollment> findByUsername(String username);
}
