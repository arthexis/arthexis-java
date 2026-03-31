package com.arthexis.platform.app.cp;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class CpChargingRealtimeService {

  private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

  public SseEmitter connect() {
    SseEmitter emitter = new SseEmitter(0L);
    emitters.add(emitter);
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(error -> emitters.remove(emitter));
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

    emitters.forEach(
        emitter -> {
          try {
            emitter.send(SseEmitter.event().name("charging-sample").data(payload));
          } catch (IOException ex) {
            emitter.completeWithError(ex);
            emitters.remove(emitter);
          }
        });
  }
}
