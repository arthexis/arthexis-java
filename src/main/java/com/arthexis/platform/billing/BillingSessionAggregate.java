package com.arthexis.platform.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "billing_session_aggregate")
public class BillingSessionAggregate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_id", nullable = false, unique = true, length = 128)
  private String sessionId;

  @Column(name = "station_id", nullable = false, length = 64)
  private String stationId;

  @Column(name = "tenant_id", nullable = false, length = 64)
  private String tenantId;

  @Column(name = "account_id", nullable = false, length = 64)
  private String accountId;

  @Column(name = "rated_wh", nullable = false)
  private long ratedWh;

  @Column(name = "rated_amount", precision = 12, scale = 4, nullable = false)
  private BigDecimal ratedAmount;

  @Column(name = "currency", nullable = false, length = 8)
  private String currency;

  @Column(name = "invoice_ready", nullable = false)
  private boolean invoiceReady;

  @Column(name = "rated_at", nullable = false)
  private Instant ratedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected BillingSessionAggregate() {}

  public BillingSessionAggregate(String sessionId, String stationId, String tenantId, String accountId) {
    this.sessionId = sessionId;
    this.stationId = stationId;
    this.tenantId = tenantId;
    this.accountId = accountId;
    this.ratedAmount = BigDecimal.ZERO;
    this.currency = "USD";
    this.ratedAt = Instant.now();
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getStationId() {
    return stationId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getAccountId() {
    return accountId;
  }

  public long getRatedWh() {
    return ratedWh;
  }

  public BigDecimal getRatedAmount() {
    return ratedAmount;
  }

  public String getCurrency() {
    return currency;
  }

  public boolean isInvoiceReady() {
    return invoiceReady;
  }

  public Instant getRatedAt() {
    return ratedAt;
  }

  public void markRated(long ratedWh, BigDecimal ratedAmount, String currency, boolean invoiceReady) {
    this.ratedWh = ratedWh;
    this.ratedAmount = ratedAmount;
    if (currency != null && !currency.isBlank()) {
      this.currency = currency;
    }
    this.invoiceReady = invoiceReady;
    this.ratedAt = Instant.now();
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
