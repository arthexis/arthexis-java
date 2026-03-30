package com.arthexis.platform.charging;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargingStationRepository extends JpaRepository<ChargingStation, Long> {
  Optional<ChargingStation> findByStationId(String stationId);
}
