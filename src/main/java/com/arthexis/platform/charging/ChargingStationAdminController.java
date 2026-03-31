package com.arthexis.platform.charging;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/chargers")
public class ChargingStationAdminController {

  private final ChargingStationService chargingStationService;

  public ChargingStationAdminController(ChargingStationService chargingStationService) {
    this.chargingStationService = chargingStationService;
  }

  @GetMapping
  public List<ChargingStationView> listStations() {
    return chargingStationService.listStations().stream().map(ChargingStationView::from).toList();
  }

  @GetMapping("/{stationId}")
  public ChargingStationView getStation(@PathVariable String stationId) {
    return ChargingStationView.from(chargingStationService.getStation(stationId));
  }

  @PutMapping("/{stationId}/status")
  public ChargingStationView updateStatus(
      @PathVariable String stationId, @RequestBody ChargingStationStatusRequest request) {
    return ChargingStationView.from(chargingStationService.upsertStatus(stationId, request.status()));
  }

  @ExceptionHandler(ChargingStationNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiError stationNotFound(ChargingStationNotFoundException ex) {
    return new ApiError("charging_station_not_found", ex.getMessage(), Instant.now());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError badRequest(IllegalArgumentException ex) {
    return new ApiError("invalid_request", ex.getMessage(), Instant.now());
  }

  public record ChargingStationView(String stationId, String status, Instant lastSeenAt) {

    private static ChargingStationView from(ChargingStation station) {
      return new ChargingStationView(station.getStationId(), station.getStatus(), station.getLastSeenAt());
    }
  }

  public record ApiError(String code, String message, Instant timestamp) {}
}
