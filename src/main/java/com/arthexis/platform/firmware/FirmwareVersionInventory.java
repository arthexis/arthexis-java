package com.arthexis.platform.firmware;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "firmware_version_inventory")
public class FirmwareVersionInventory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false, length = 64)
  private String tenantId;

  @Column(name = "version", nullable = false, length = 64)
  private String version;

  @Column(name = "release_channel", nullable = false, length = 32)
  private String releaseChannel;

  @Column(name = "rollout_notes", length = 1024)
  private String rolloutNotes;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected FirmwareVersionInventory() {}
}
