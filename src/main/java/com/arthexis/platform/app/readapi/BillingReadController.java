package com.arthexis.platform.app.readapi;

import com.arthexis.platform.billing.BillingSessionAggregate;
import com.arthexis.platform.billing.BillingSessionAggregateRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
  public List<BillingSessionAggregate> invoiceReady() {
    return repository.findByInvoiceReadyTrueOrderByRatedAtDesc();
  }
}
