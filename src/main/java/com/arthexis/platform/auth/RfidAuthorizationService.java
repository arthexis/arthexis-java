package com.arthexis.platform.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RfidAuthorizationService implements RfidAuthorizationGateway {

  private final RfidCardRepository cardRepository;
  private final EnergyAccountRepository accountRepository;
  private final ChargePointLoginSessionRepository loginSessionRepository;
  private final RfidAuthProperties properties;

  public RfidAuthorizationService(
      RfidCardRepository cardRepository,
      EnergyAccountRepository accountRepository,
      ChargePointLoginSessionRepository loginSessionRepository,
      RfidAuthProperties properties) {
    this.cardRepository = cardRepository;
    this.accountRepository = accountRepository;
    this.loginSessionRepository = loginSessionRepository;
    this.properties = properties;
  }

  @Transactional
  public RfidCard linkCardToAccount(RfidCardLinkRequest request) {
    EnergyAccount account =
        accountRepository
            .findByAccountExternalId(request.accountExternalId())
            .orElseGet(
                () ->
                    accountRepository.save(
                        new EnergyAccount(
                            request.accountExternalId(), request.accountDisplayName(), request.email())));

    RfidCard card =
        cardRepository
            .findByCardUid(request.cardUid())
            .orElseGet(() -> new RfidCard(request.cardUid(), defaultMode(request.authMode())));

    card.setAuthMode(defaultMode(request.authMode()));
    card.linkToAccount(account, request.requireApproval());
    return cardRepository.save(card);
  }

  @Transactional
  public RfidCard approveCardLink(String cardUid) {
    RfidCard card =
        cardRepository.findByCardUid(cardUid).orElseThrow(() -> new IllegalArgumentException("RFID card not found"));
    card.approveLink();
    return cardRepository.save(card);
  }

  @Transactional
  public AuthorizationDecision authorize(String stationId, String cardUid) {
    Optional<RfidCard> cardCandidate = cardRepository.findByCardUid(cardUid);
    if (cardCandidate.isEmpty() || !cardCandidate.get().isActive()) {
      return new AuthorizationDecision(false, "Invalid", "NONE", null, null, "rfid_card_unknown");
    }

    RfidCard card = cardCandidate.get();
    if (card.isLinkApprovalRequired() && !card.isLinkApproved()) {
      String loginUrl = issueLoginQr(stationId, cardUid);
      return new AuthorizationDecision(
          false, "Blocked", card.getAuthMode().name(), accountId(card), loginUrl, "link_waiting_approval");
    }

    if (card.getAuthMode() == RfidAuthMode.DIRECT) {
      return new AuthorizationDecision(true, "Accepted", "DIRECT", accountId(card), null, "direct_authorized");
    }

    if (hasRecentCompletedLogin(stationId, cardUid)) {
      return new AuthorizationDecision(
          true, "Accepted", "ACCOUNT_LOGIN", accountId(card), null, "indirect_login_verified");
    }

    String loginUrl = issueLoginQr(stationId, cardUid);
    return new AuthorizationDecision(
        false,
        "ConcurrentTx",
        "ACCOUNT_LOGIN",
        accountId(card),
        loginUrl,
        "user_login_required");
  }

  @Transactional
  public String issueLoginQr(String stationId, String cardUid) {
    String token = UUID.randomUUID().toString();
    ChargePointLoginSession session =
        new ChargePointLoginSession(token, stationId, cardUid, Instant.now().plus(properties.getLoginSessionTtl()));
    loginSessionRepository.save(session);
    return properties.getLoginBaseUrl() + "?token=" + token;
  }

  @Transactional
  public AuthorizationDecision completeLogin(String token, String accountExternalId) {
    ChargePointLoginSession session =
        loginSessionRepository
            .findByLoginToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Login token not found"));

    if (session.getExpiresAt().isBefore(Instant.now())) {
      session.markExpired();
      loginSessionRepository.save(session);
      return new AuthorizationDecision(false, "Expired", "ACCOUNT_LOGIN", null, null, "login_session_expired");
    }

    EnergyAccount account =
        accountRepository
            .findByAccountExternalId(accountExternalId)
            .orElseThrow(() -> new IllegalArgumentException("Energy account not found"));

    RfidCard card =
        cardRepository
            .findByCardUid(session.getCardUid())
            .orElseThrow(() -> new IllegalArgumentException("RFID card not found"));

    if (card.getEnergyAccount() == null || !Objects.equals(card.getEnergyAccount().getId(), account.getId())) {
      card.linkToAccount(account, card.isLinkApprovalRequired());
      cardRepository.save(card);
    }

    session.markCompleted(accountExternalId);
    loginSessionRepository.save(session);

    if (card.isLinkApprovalRequired() && !card.isLinkApproved()) {
      return new AuthorizationDecision(false, "Blocked", "ACCOUNT_LOGIN", accountExternalId, null, "awaiting_approval");
    }

    return new AuthorizationDecision(
        true, "Accepted", "ACCOUNT_LOGIN", accountExternalId, null, "login_completed");
  }

  private RfidAuthMode defaultMode(RfidAuthMode mode) {
    return mode == null ? RfidAuthMode.DIRECT : mode;
  }

  private boolean hasRecentCompletedLogin(String stationId, String cardUid) {
    return loginSessionRepository
        .findTopByStationIdAndCardUidAndStatusOrderByUpdatedAtDesc(
            stationId, cardUid, LoginSessionStatus.COMPLETED)
        .map(
            session ->
                session
                    .getUpdatedAt()
                    .isAfter(Instant.now().minus(properties.getTrustedLoginWindow())))
        .orElse(false);
  }

  private String accountId(RfidCard card) {
    return card.getEnergyAccount() == null ? null : card.getEnergyAccount().getAccountExternalId();
  }
}
