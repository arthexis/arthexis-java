package com.arthexis.platform.security.mfa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminWebauthnCredentialRepository extends JpaRepository<AdminWebauthnCredential, Long> {

  List<AdminWebauthnCredential> findByUsername(String username);

  Optional<AdminWebauthnCredential> findByUsernameAndCredentialId(String username, String credentialId);
}
