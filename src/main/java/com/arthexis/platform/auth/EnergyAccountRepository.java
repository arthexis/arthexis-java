package com.arthexis.platform.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnergyAccountRepository extends JpaRepository<EnergyAccount, Long> {

  Optional<EnergyAccount> findByAccountExternalId(String accountExternalId);
}
