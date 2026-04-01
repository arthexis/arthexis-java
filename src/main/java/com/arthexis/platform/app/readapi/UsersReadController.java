package com.arthexis.platform.app.readapi;

import com.arthexis.platform.users.UserAccountMetadata;
import com.arthexis.platform.users.UserAccountMetadataRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
  public List<UserAccountMetadata> accounts(@RequestParam String tenantId) {
    return repository.findByTenantIdOrderByUpdatedAtDesc(tenantId);
  }
}
