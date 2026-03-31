package com.arthexis.platform.charging;

import java.util.Arrays;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ChargingStationCli implements ApplicationRunner {

  private final ChargingStationService chargingStationService;

  public ChargingStationCli(ChargingStationService chargingStationService) {
    this.chargingStationService = chargingStationService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!args.containsOption("charger-cli")) {
      return;
    }

    String command = args.getOptionValues("charger-cli").getFirst();
    switch (command) {
      case "list" -> list();
      case "get" -> get(requiredArg(args, "station-id"));
      case "set-status" ->
          setStatus(requiredArg(args, "station-id"), requiredArg(args, "status"));
      default ->
          throw new IllegalArgumentException(
              "Unsupported --charger-cli command. Expected one of: "
                  + Arrays.asList("list", "get", "set-status"));
    }
  }

  private void list() {
    chargingStationService
        .listStations()
        .forEach(
            station ->
                System.out.printf(
                    "%s\t%s\t%s%n",
                    station.getStationId(), station.getStatus(), station.getLastSeenAt()));
  }

  private void get(String stationId) {
    ChargingStation station = chargingStationService.getStation(stationId);
    System.out.printf("%s\t%s\t%s%n", station.getStationId(), station.getStatus(), station.getLastSeenAt());
  }

  private void setStatus(String stationId, String status) {
    ChargingStation station = chargingStationService.upsertStatus(stationId, status);
    System.out.printf(
        "updated\t%s\t%s\t%s%n", station.getStationId(), station.getStatus(), station.getLastSeenAt());
  }

  private String requiredArg(ApplicationArguments args, String key) {
    if (!args.containsOption(key) || args.getOptionValues(key).isEmpty()) {
      throw new IllegalArgumentException("Missing required option --" + key);
    }
    return args.getOptionValues(key).getFirst();
  }
}
