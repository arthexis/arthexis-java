package com.arthexis.platform.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminTotpFactorRepository extends JpaRepository<AdminTotpFactor, Long> {

  Optional<AdminTotpFactor> findByUsername(String username);
}
