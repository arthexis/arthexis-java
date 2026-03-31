package com.arthexis.platform.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/rfid")
public class RfidAuthorizationController {

  private final RfidAuthorizationService authorizationService;

  public RfidAuthorizationController(RfidAuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @PostMapping("/cards/link")
  public RfidCard linkCard(@RequestBody RfidCardLinkRequest request) {
    return authorizationService.linkCardToAccount(request);
  }

  @PostMapping("/cards/{cardUid}/approve")
  public RfidCard approve(@PathVariable String cardUid) {
    return authorizationService.approveCardLink(cardUid);
  }

  @PostMapping("/authorize/{stationId}/{cardUid}")
  public AuthorizationDecision authorize(@PathVariable String stationId, @PathVariable String cardUid) {
    return authorizationService.authorize(stationId, cardUid);
  }

  @GetMapping("/charge-points/{stationId}/qr/{cardUid}")
  public String loginQr(@PathVariable String stationId, @PathVariable String cardUid) {
    return authorizationService.issueLoginQr(stationId, cardUid);
  }

  @PostMapping("/login/{token}")
  public AuthorizationDecision completeLogin(
      @PathVariable String token, @RequestBody LoginCompletionRequest request) {
    return authorizationService.completeLogin(token, request.accountExternalId());
  }
}
