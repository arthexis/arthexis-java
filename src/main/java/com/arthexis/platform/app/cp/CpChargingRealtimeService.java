package com.arthexis.platform.app.cp;

import com.arthexis.platform.telemetry.CpChargingSampleEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class CpChargingRealtimeService {

  private static final long SSE_TIMEOUT_MS = 30_000L;
  private final Map<String, Set<SseEmitter>> emittersByStationId = new ConcurrentHashMap<>();

  public SseEmitter connect(String stationId) {
    SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
    emittersByStationId.computeIfAbsent(stationId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
    emitter.onCompletion(() -> removeEmitter(stationId, emitter));
    emitter.onTimeout(() -> removeEmitter(stationId, emitter));
    emitter.onError(error -> removeEmitter(stationId, emitter));
    return emitter;
  }

  @EventListener
  public void onSample(CpChargingSampleEvent event) {
    Map<String, Object> payload =
        Map.of(
            "stationId", event.stationId(),
            "metricName", event.metricName(),
            "metricValue", event.metricValue(),
            "unit", event.unit() == null ? "" : event.unit(),
            "sampledAt", event.sampledAt() == null ? Instant.now().toString() : event.sampledAt().toString());

    emittersByStationId.getOrDefault(event.stationId(), Set.of()).forEach(
        emitter -> {
          try {
            emitter.send(SseEmitter.event().name("charging-sample").data(payload));
          } catch (IOException ex) {
            emitter.completeWithError(ex);
            removeEmitter(event.stationId(), emitter);
          }
        });
  }

  private void removeEmitter(String stationId, SseEmitter emitter) {
    emittersByStationId.computeIfPresent(
        stationId,
        (ignored, emitters) -> {
          emitters.remove(emitter);
          return emitters.isEmpty() ? null : emitters;
        });
  }
}
