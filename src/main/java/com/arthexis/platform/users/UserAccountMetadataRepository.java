package com.arthexis.platform.users;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountMetadataRepository extends JpaRepository<UserAccountMetadata, Long> {

  Optional<UserAccountMetadata> findByTenantIdAndAccountId(String tenantId, String accountId);

  List<UserAccountMetadata> findByTenantIdOrderByUpdatedAtDesc(String tenantId);
}
