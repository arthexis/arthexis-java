package com.arthexis.platform.users;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserIdentityService {

  private final UserAccountMetadataRepository repository;
  private final ApplicationEventPublisher eventPublisher;

  public UserIdentityService(
      UserAccountMetadataRepository repository, ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public UserAccountMetadata upsert(
      String tenantId, String accountId, String operatorId, String customerId, String identityState) {
    UserAccountMetadata metadata =
        repository
            .findByTenantIdAndAccountId(tenantId, accountId)
            .orElseGet(() -> new UserAccountMetadata(tenantId, accountId));

    metadata.updateIdentity(operatorId, customerId, identityState);
    UserAccountMetadata persisted = repository.save(metadata);

    eventPublisher.publishEvent(
        new UserIdentityMetadataChangedEvent(
            persisted.getTenantId(),
            persisted.getAccountId(),
            persisted.getOperatorId(),
            persisted.getCustomerId(),
            persisted.getIdentityState(),
            persisted.getUpdatedAt()));

    return persisted;
  }
}
