package com.arthexis.platform.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RfidCardRepository extends JpaRepository<RfidCard, Long> {

  Optional<RfidCard> findByCardUid(String cardUid);
}
