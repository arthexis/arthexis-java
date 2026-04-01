package com.arthexis.platform.transfer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arthexis.ocpp.ftp")
public class OcppFtpServerProperties {

  private boolean enabled;

  private String bindAddress = "0.0.0.0";

    private int port = 2121;

  private String publicHost = "localhost";

  private String rootDirectory = "./var/ocpp-ftp";

  private List<Binding> bindings = new ArrayList<>();

  public boolean enabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String bindAddress() {
    return bindAddress;
  }

  public void setBindAddress(String bindAddress) {
    this.bindAddress = bindAddress;
  }

  public int port() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String publicHost() {
    return publicHost;
  }

  public void setPublicHost(String publicHost) {
    this.publicHost = publicHost;
  }

  public Path rootDirectory() {
    return Path.of(rootDirectory).toAbsolutePath().normalize();
  }

  public void setRootDirectory(String rootDirectory) {
    this.rootDirectory = rootDirectory;
  }

  public List<Binding> bindings() {
    return bindings;
  }

  public void setBindings(List<Binding> bindings) {
    this.bindings = bindings;
  }

  public static class Binding {

    private String id;

    private String username;

    private String password;

    private List<String> chargerIds = new ArrayList<>();

    public String id() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String username() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String password() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public List<String> chargerIds() {
      return chargerIds;
    }

    public void setChargerIds(List<String> chargerIds) {
      this.chargerIds = chargerIds;
    }
  }
}
