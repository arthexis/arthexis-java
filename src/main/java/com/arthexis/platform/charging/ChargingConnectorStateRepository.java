package com.arthexis.platform.charging;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargingConnectorStateRepository extends JpaRepository<ChargingConnectorState, Long> {

  Optional<ChargingConnectorState> findByStationIdAndEvseIdAndConnectorId(
      String stationId, int evseId, int connectorId);

  List<ChargingConnectorState> findByStationIdOrderByEvseIdAscConnectorIdAsc(String stationId);
}
