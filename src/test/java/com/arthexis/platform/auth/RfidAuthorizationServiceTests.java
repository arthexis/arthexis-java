package com.arthexis.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@DataJpaTest
@EnableConfigurationProperties(RfidAuthProperties.class)
@Import(RfidAuthorizationService.class)
class RfidAuthorizationServiceTests {

  @Autowired private RfidAuthorizationService service;

  @Test
  void acceptsDirectAuthorizationForLinkedCard() {
    service.linkCardToAccount(
        new RfidCardLinkRequest(
            "CARD-001", "acct-100", RfidAuthMode.DIRECT, false, "A User", "a@example.com"));

    AuthorizationDecision decision = service.authorize("CP-01", "CARD-001");

    assertThat(decision.accepted()).isTrue();
    assertThat(decision.ocppStatus()).isEqualTo("Accepted");
    assertThat(decision.authMode()).isEqualTo("DIRECT");
  }

  @Test
  void requiresIndirectLoginForAccountLoginModeThenAcceptsAfterCompletion() {
    service.linkCardToAccount(
        new RfidCardLinkRequest(
            "CARD-002",
            "acct-200",
            RfidAuthMode.ACCOUNT_LOGIN,
            false,
            "B User",
            "b@example.com"));

    AuthorizationDecision initial = service.authorize("CP-02", "CARD-002");

    assertThat(initial.accepted()).isFalse();
    assertThat(initial.ocppStatus()).isEqualTo("Blocked");
    assertThat(initial.loginUrl()).contains("token=");

    String token = initial.loginUrl().substring(initial.loginUrl().indexOf("token=") + 6);
    AuthorizationDecision completed = service.completeLogin(token, "acct-200");
    assertThat(completed.accepted()).isTrue();

    AuthorizationDecision afterLogin = service.authorize("CP-02", "CARD-002");
    assertThat(afterLogin.accepted()).isTrue();
    assertThat(afterLogin.reason()).isEqualTo("indirect_login_verified");
  }

  @Test
  void rejectsReusedLoginTokens() {
    service.linkCardToAccount(
        new RfidCardLinkRequest(
            "CARD-004",
            "acct-400",
            RfidAuthMode.ACCOUNT_LOGIN,
            false,
            "D User",
            "d@example.com"));

    AuthorizationDecision initial = service.authorize("CP-04", "CARD-004");
    String token = initial.loginUrl().substring(initial.loginUrl().indexOf("token=") + 6);

    AuthorizationDecision firstCompletion = service.completeLogin(token, "acct-400");
    AuthorizationDecision replayCompletion = service.completeLogin(token, "acct-400");

    assertThat(firstCompletion.accepted()).isTrue();
    assertThat(replayCompletion.accepted()).isFalse();
    assertThat(replayCompletion.reason()).isEqualTo("login_session_not_pending");
  }

  @Test
  void doesNotAllowLoginCompletionToRelinkCardToDifferentAccount() {
    service.linkCardToAccount(
        new RfidCardLinkRequest(
            "CARD-005",
            "acct-500",
            RfidAuthMode.ACCOUNT_LOGIN,
            false,
            "E User",
            "e@example.com"));

    service.linkCardToAccount(
        new RfidCardLinkRequest(
            "CARD-006",
            "acct-600",
            RfidAuthMode.DIRECT,
            false,
            "F User",
            "f@example.com"));

    AuthorizationDecision initial = service.authorize("CP-05", "CARD-005");
    String token = initial.loginUrl().substring(initial.loginUrl().indexOf("token=") + 6);

    AuthorizationDecision rejected = service.completeLogin(token, "acct-600");

    assertThat(rejected.accepted()).isFalse();
    assertThat(rejected.reason()).isEqualTo("card_already_linked");
  }

  @Test
  void keepsCardBlockedUntilApprovalWhenApprovalIsRequired() {
    service.linkCardToAccount(
        new RfidCardLinkRequest(
            "CARD-003",
            "acct-300",
            RfidAuthMode.DIRECT,
            true,
            "C User",
            "c@example.com"));

    AuthorizationDecision blocked = service.authorize("CP-03", "CARD-003");
    assertThat(blocked.accepted()).isFalse();
    assertThat(blocked.ocppStatus()).isEqualTo("Blocked");

    service.approveCardLink("CARD-003");

    AuthorizationDecision accepted = service.authorize("CP-03", "CARD-003");
    assertThat(accepted.accepted()).isTrue();
    assertThat(accepted.ocppStatus()).isEqualTo("Accepted");
  }
}
