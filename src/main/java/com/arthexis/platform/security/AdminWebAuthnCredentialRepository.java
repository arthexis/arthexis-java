package com.arthexis.platform.security;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminWebAuthnCredentialRepository extends JpaRepository<AdminWebAuthnCredential, Long> {

  List<AdminWebAuthnCredential> findByUsername(String username);

  Optional<AdminWebAuthnCredential> findByUsernameAndCredentialId(String username, String credentialId);
}
