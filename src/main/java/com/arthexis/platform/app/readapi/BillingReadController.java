package com.arthexis.platform.app.readapi;

import com.arthexis.platform.billing.BillingSessionAggregate;
import com.arthexis.platform.billing.BillingSessionAggregateRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/read/billing")
@ConditionalOnProperty(
    prefix = "arthexis.features.read-api",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
public class BillingReadController {

  private final BillingSessionAggregateRepository repository;

  public BillingReadController(BillingSessionAggregateRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/invoice-ready")
  public Page<BillingSessionAggregateResponse> invoiceReady(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
    return repository
        .findByInvoiceReadyTrue(PageRequest.of(page, size, Sort.by("ratedAt").descending()))
        .map(BillingSessionAggregateResponse::from);
  }

  public record BillingSessionAggregateResponse(
      String sessionId,
      String stationId,
      String tenantId,
      String accountId,
      long ratedWh,
      BigDecimal ratedAmount,
      String currency,
      boolean invoiceReady,
      Instant ratedAt) {

    static BillingSessionAggregateResponse from(BillingSessionAggregate aggregate) {
      return new BillingSessionAggregateResponse(
          aggregate.getSessionId(),
          aggregate.getStationId(),
          aggregate.getTenantId(),
          aggregate.getAccountId(),
          aggregate.getRatedWh(),
          aggregate.getRatedAmount(),
          aggregate.getCurrency(),
          aggregate.isInvoiceReady(),
          aggregate.getRatedAt());
    }
  }
}
