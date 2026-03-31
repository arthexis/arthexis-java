package com.arthexis.platform.charging;

public class ChargingStationNotFoundException extends RuntimeException {

  public ChargingStationNotFoundException(String stationId) {
    super("Charging station not found: " + stationId);
  }
}
