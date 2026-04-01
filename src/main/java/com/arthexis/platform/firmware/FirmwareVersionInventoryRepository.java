package com.arthexis.platform.firmware;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FirmwareVersionInventoryRepository
    extends JpaRepository<FirmwareVersionInventory, Long> {}
