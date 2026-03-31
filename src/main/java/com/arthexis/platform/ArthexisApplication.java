package com.arthexis.platform;

import org.springframework.boot.SpringApplication;
import com.arthexis.platform.ocpp.OcppCommandDispatchProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.modulith.Modulith;

@SpringBootApplication
@EnableConfigurationProperties(OcppCommandDispatchProperties.class)
@Modulith(systemName = "Arthexis Java")
public class ArthexisApplication {

  public static void main(String[] args) {
    SpringApplication.run(ArthexisApplication.class, args);
  }
}
