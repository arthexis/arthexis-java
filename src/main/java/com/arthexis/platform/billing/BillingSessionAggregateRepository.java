package com.arthexis.platform.billing;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingSessionAggregateRepository extends JpaRepository<BillingSessionAggregate, Long> {

  Optional<BillingSessionAggregate> findBySessionId(String sessionId);

  List<BillingSessionAggregate> findByInvoiceReadyTrueOrderByRatedAtDesc();

  Page<BillingSessionAggregate> findByInvoiceReadyTrue(Pageable pageable);
}
