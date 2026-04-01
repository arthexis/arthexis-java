package com.arthexis.platform.billing;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

  private final BillingSessionAggregateRepository repository;
  private final ApplicationEventPublisher eventPublisher;

  public BillingService(
      BillingSessionAggregateRepository repository, ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public BillingSessionAggregate rateUsage(
      String sessionId,
      String stationId,
      String tenantId,
      String accountId,
      long ratedWh,
      BigDecimal ratedAmount,
      String currency,
      boolean invoiceReady) {
    BillingSessionAggregate aggregate =
        repository
            .findBySessionId(sessionId)
            .orElseGet(() -> new BillingSessionAggregate(sessionId, stationId, tenantId, accountId));

    aggregate.markRated(ratedWh, ratedAmount, currency, invoiceReady);
    BillingSessionAggregate persisted = repository.save(aggregate);

    eventPublisher.publishEvent(
        new BillingSessionRatedEvent(
            persisted.getSessionId(),
            persisted.getStationId(),
            persisted.getTenantId(),
            persisted.getAccountId(),
            persisted.getRatedWh(),
            persisted.getRatedAmount(),
            persisted.getCurrency(),
            persisted.isInvoiceReady(),
            Instant.now()));

    return persisted;
  }
}
