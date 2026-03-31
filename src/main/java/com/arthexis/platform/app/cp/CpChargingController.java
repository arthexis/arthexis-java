package com.arthexis.platform.app.cp;

import com.arthexis.platform.telemetry.TelemetrySample;
import com.arthexis.platform.telemetry.TelemetrySampleRepository;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@RequestMapping("/cp/charging")
public class CpChargingController {

  private final TelemetrySampleRepository telemetrySampleRepository;
  private final CpChargingRealtimeService realtimeService;

  public CpChargingController(
      TelemetrySampleRepository telemetrySampleRepository, CpChargingRealtimeService realtimeService) {
    this.telemetrySampleRepository = telemetrySampleRepository;
    this.realtimeService = realtimeService;
  }

  @GetMapping
  public String view() {
    return "redirect:/cp/index.html";
  }

  @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @ResponseBody
  public SseEmitter stream(@RequestParam String stationId, Authentication authentication) {
    enforceStationAccess(authentication, stationId);
    return realtimeService.connect(stationId);
  }

  @GetMapping(path = "/history", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<Map<String, Object>> history(
      @RequestParam String stationId,
      @RequestParam(defaultValue = "Power.Active.Import") String metricName,
      Authentication authentication) {
    enforceStationAccess(authentication, stationId);
    return telemetrySampleRepository.findTop120ByStationIdAndMetricNameOrderBySampledAtDesc(
            stationId, metricName)
        .stream()
        .sorted((left, right) -> left.getSampledAt().compareTo(right.getSampledAt()))
        .map(this::toViewModel)
        .toList();
  }

  @GetMapping(path = "/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<String> metrics(@RequestParam String stationId, Authentication authentication) {
    enforceStationAccess(authentication, stationId);
    return telemetrySampleRepository.findTop100ByStationIdOrderBySampledAtDesc(stationId).stream()
        .map(TelemetrySample::getMetricName)
        .distinct()
        .toList();
  }

  private void enforceStationAccess(Principal principal, String stationId) {
    if (principal instanceof Authentication authentication
        && authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_CP_CUSTOMER"::equals)
        && !authentication.getName().equals(stationId)) {
      throw new AccessDeniedException("Not allowed to access requested station");
    }
  }

  private Map<String, Object> toViewModel(TelemetrySample sample) {
    return Map.of(
        "stationId", sample.getStationId(),
        "metricName", sample.getMetricName(),
        "metricValue", sample.getMetricValue(),
        "unit", sample.getUnit() == null ? "" : sample.getUnit(),
        "sampledAt", sample.getSampledAt().toString());
  }
}
