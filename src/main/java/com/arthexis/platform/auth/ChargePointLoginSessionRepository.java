package com.arthexis.platform.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargePointLoginSessionRepository extends JpaRepository<ChargePointLoginSession, Long> {

  Optional<ChargePointLoginSession> findByLoginToken(String loginToken);

  Optional<ChargePointLoginSession> findTopByStationIdAndCardUidAndStatusOrderByUpdatedAtDesc(
      String stationId, String cardUid, LoginSessionStatus status);
}
