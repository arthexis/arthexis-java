package com.arthexis.platform.app.cp;

import com.arthexis.platform.telemetry.TelemetrySample;
import com.arthexis.platform.telemetry.TelemetrySampleRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
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
    return "redirect:/cp/charging/index.html";
  }

  @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @ResponseBody
  public SseEmitter stream() {
    return realtimeService.connect();
  }

  @GetMapping(path = "/history", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<Map<String, Object>> history(
      @RequestParam String stationId, @RequestParam(defaultValue = "Power.Active.Import") String metricName) {
    return telemetrySampleRepository.findTop120ByStationIdAndMetricNameOrderBySampledAtAsc(
            stationId, metricName)
        .stream()
        .map(this::toViewModel)
        .toList();
  }

  @GetMapping(path = "/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<String> metrics(@RequestParam String stationId) {
    return telemetrySampleRepository.findTop10ByStationIdOrderBySampledAtDesc(stationId).stream()
        .map(TelemetrySample::getMetricName)
        .distinct()
        .toList();
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
