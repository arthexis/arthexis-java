package com.arthexis.platform.transfer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class OcppFtpUrlFactory {

  private final OcppFtpServerProperties properties;
  private final OcppFtpBindingRegistry bindings;

  public OcppFtpUrlFactory(OcppFtpServerProperties properties, OcppFtpBindingRegistry bindings) {
    this.properties = properties;
    this.bindings = bindings;
  }

  public URI buildDownloadUrl(String chargerId, String relativePath) {
    OcppFtpServerProperties.Binding binding =
        bindings
            .bindingForCharger(chargerId)
            .orElseThrow(
                () -> new IllegalArgumentException("No FTP binding configured for charger: " + chargerId));
    String normalizedPath = normalize(relativePath);
    String path = String.format("/%s/%s", chargerId, normalizedPath);
    try {
      return new URI(
          "ftp",
          String.format(Locale.ROOT, "%s:%s", binding.username(), binding.password()),
          properties.publicHost(),
          properties.port(),
          path,
          null,
          null);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Failed to build FTP URI", e);
    }
  }

  private String normalize(String relativePath) {
    String candidate = relativePath == null ? "" : relativePath.trim();
    candidate = candidate.replace('\\', '/');
    while (candidate.startsWith("/")) {
      candidate = candidate.substring(1);
    }
    if (candidate.isBlank() || candidate.contains("..")) {
      throw new IllegalArgumentException("Invalid FTP relative path: " + relativePath);
    }
    return candidate;
  }
}
