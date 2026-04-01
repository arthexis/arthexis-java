package com.arthexis.platform.users;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountMetadataRepository extends JpaRepository<UserAccountMetadata, Long> {

  Optional<UserAccountMetadata> findByTenantIdAndAccountId(String tenantId, String accountId);

  List<UserAccountMetadata> findByTenantIdOrderByUpdatedAtDesc(String tenantId);

  Page<UserAccountMetadata> findByTenantId(String tenantId, Pageable pageable);
}
