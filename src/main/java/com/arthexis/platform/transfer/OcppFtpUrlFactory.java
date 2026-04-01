package com.arthexis.platform.transfer;

import java.net.URI;
import java.nio.charset.StandardCharsets;
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
    String encodedUserInfo =
        String.format(
            Locale.ROOT,
            "%s:%s",
            encodeUserInfoComponent(binding.username()),
            encodeUserInfoComponent(binding.password()));
    String uri =
        String.format(
            Locale.ROOT,
            "ftp://%s@%s:%d%s",
            encodedUserInfo,
            properties.publicHost(),
            properties.port(),
            path);
    return URI.create(uri);
  }

  private String encodeUserInfoComponent(String value) {
    StringBuilder encoded = new StringBuilder();
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    for (byte candidate : bytes) {
      int unsigned = candidate & 0xFF;
      if (isUnreserved(unsigned)) {
        encoded.append((char) unsigned);
      } else {
        encoded.append(String.format(Locale.ROOT, "%%%02X", unsigned));
      }
    }
    return encoded.toString();
  }

  private boolean isUnreserved(int value) {
    return (value >= 'a' && value <= 'z')
        || (value >= 'A' && value <= 'Z')
        || (value >= '0' && value <= '9')
        || value == '-'
        || value == '.'
        || value == '_'
        || value == '~';
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
