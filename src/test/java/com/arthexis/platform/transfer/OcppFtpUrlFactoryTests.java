package com.arthexis.platform.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class OcppFtpUrlFactoryTests {

  @Test
  void buildsCredentialedFtpUrlFromChargerBinding() {
    OcppFtpServerProperties properties = new OcppFtpServerProperties();
    properties.setPublicHost("ftp.arthexis.local");
    properties.setPort(2211);

    OcppFtpServerProperties.Binding binding = new OcppFtpServerProperties.Binding();
    binding.setId("north-yard");
    binding.setUsername("north");
    binding.setPassword("pw");
    binding.setChargerIds(List.of("cp-ny-01"));
    properties.setBindings(List.of(binding));

    OcppFtpBindingRegistry registry = new OcppFtpBindingRegistry(properties);
    registry.initialize();

    OcppFtpUrlFactory factory = new OcppFtpUrlFactory(properties, registry);
    URI url = factory.buildDownloadUrl("cp-ny-01", "firmware/arthexis-1.2.3.bin");

    assertThat(url.toString())
        .isEqualTo("ftp://north:pw@ftp.arthexis.local:2211/cp-ny-01/firmware/arthexis-1.2.3.bin");
  }

  @Test
  void rejectsDirectoryTraversalSegments() {
    OcppFtpServerProperties properties = new OcppFtpServerProperties();

    OcppFtpServerProperties.Binding binding = new OcppFtpServerProperties.Binding();
    binding.setId("alpha");
    binding.setUsername("alpha");
    binding.setPassword("pw");
    binding.setChargerIds(List.of("cp-1"));
    properties.setBindings(List.of(binding));

    OcppFtpBindingRegistry registry = new OcppFtpBindingRegistry(properties);
    registry.initialize();

    OcppFtpUrlFactory factory = new OcppFtpUrlFactory(properties, registry);
    assertThatThrownBy(() -> factory.buildDownloadUrl("cp-1", "../escape.bin"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
