package com.arthexis.platform.app.readapi;

import com.arthexis.platform.users.UserAccountMetadata;
import com.arthexis.platform.users.UserAccountMetadataRepository;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/read/users")
@ConditionalOnProperty(
    prefix = "arthexis.features.read-api",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
public class UsersReadController {

  private final UserAccountMetadataRepository repository;

  public UsersReadController(UserAccountMetadataRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/accounts")
  public Page<UserAccountMetadataResponse> accounts(
      @RequestParam String tenantId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return repository
        .findByTenantId(tenantId, PageRequest.of(page, size, Sort.by("updatedAt").descending()))
        .map(UserAccountMetadataResponse::from);
  }

  public record UserAccountMetadataResponse(
      String tenantId,
      String accountId,
      String operatorId,
      String customerId,
      String identityState,
      Instant createdAt,
      Instant updatedAt) {

    static UserAccountMetadataResponse from(UserAccountMetadata metadata) {
      return new UserAccountMetadataResponse(
          metadata.getTenantId(),
          metadata.getAccountId(),
          metadata.getOperatorId(),
          metadata.getCustomerId(),
          metadata.getIdentityState(),
          metadata.getCreatedAt(),
          metadata.getUpdatedAt());
    }
  }
}
