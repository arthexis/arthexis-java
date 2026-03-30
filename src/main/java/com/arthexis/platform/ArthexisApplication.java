package com.arthexis.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;

@SpringBootApplication
@Modulith(systemName = "Arthexis Java")
public class ArthexisApplication {

  public static void main(String[] args) {
    SpringApplication.run(ArthexisApplication.class, args);
  }
}
