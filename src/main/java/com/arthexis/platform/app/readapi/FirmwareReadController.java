package com.arthexis.platform.app.readapi;

import com.arthexis.platform.firmware.FirmwareRolloutStatus;
import com.arthexis.platform.firmware.FirmwareRolloutStatusRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/read/firmware")
@ConditionalOnProperty(
    prefix = "arthexis.features.read-api",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
public class FirmwareReadController {

  private final FirmwareRolloutStatusRepository repository;

  public FirmwareReadController(FirmwareRolloutStatusRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/rollouts")
  public List<FirmwareRolloutStatus> rollouts(@RequestParam String tenantId) {
    return repository.findByTenantIdOrderByUpdatedAtDesc(tenantId);
  }
}
