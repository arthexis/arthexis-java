package com.arthexis.platform.transfer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerConfigurationException;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.springframework.stereotype.Component;

@Component
public class OcppFtpServerLifecycle {

  private final OcppFtpServerProperties properties;
  private final OcppFtpBindingRegistry bindingRegistry;
  private FtpServer ftpServer;

  public OcppFtpServerLifecycle(
      OcppFtpServerProperties properties, OcppFtpBindingRegistry bindingRegistry) {
    this.properties = properties;
    this.bindingRegistry = bindingRegistry;
  }

  @jakarta.annotation.PostConstruct
  void start() {
    if (!properties.enabled() || bindingRegistry.allBindings().isEmpty()) {
      return;
    }

    FtpServerFactory serverFactory = new FtpServerFactory();
    serverFactory.setUserManager(new InMemoryOcppUserManager(properties, bindingRegistry));

    ListenerFactory listenerFactory = new ListenerFactory();
    listenerFactory.setPort(properties.port());
    listenerFactory.setServerAddress(properties.bindAddress());
    serverFactory.addListener("default", listenerFactory.createListener());

    this.ftpServer = serverFactory.createServer();
    try {
      ftpServer.start();
    } catch (FtpServerConfigurationException | org.apache.ftpserver.ftplet.FtpException e) {
      throw new IllegalStateException("Failed to start OCPP FTP server", e);
    }
  }

  @jakarta.annotation.PreDestroy
  void stop() {
    if (ftpServer != null && !ftpServer.isStopped()) {
      ftpServer.stop();
    }
  }

  static class InMemoryOcppUserManager implements UserManager {

    private final OcppFtpServerProperties properties;
    private final Map<String, OcppFtpServerProperties.Binding> byUsername;

    InMemoryOcppUserManager(
        OcppFtpServerProperties properties, OcppFtpBindingRegistry bindingRegistry) {
      this.properties = properties;
      this.byUsername =
          bindingRegistry.allBindings().stream()
              .peek(this::prepareHome)
              .collect(
                  Collectors.toUnmodifiableMap(
                      OcppFtpServerProperties.Binding::username, Function.identity()));
    }

    @Override
    public User getUserByName(String username) {
      OcppFtpServerProperties.Binding binding = byUsername.get(username);
      return binding == null ? null : toUser(binding);
    }

    @Override
    public String[] getAllUserNames() {
      return byUsername.keySet().toArray(String[]::new);
    }

    @Override
    public void delete(String username) {
      throw new UnsupportedOperationException("Static OCPP FTP bindings do not support runtime delete");
    }

    @Override
    public void save(User user) {
      throw new UnsupportedOperationException("Static OCPP FTP bindings do not support runtime save");
    }

    @Override
    public boolean doesExist(String username) {
      return getUserByName(username) != null;
    }

    @Override
    public User authenticate(Authentication authentication) throws AuthenticationFailedException {
      if (authentication instanceof AnonymousAuthentication) {
        throw new AuthenticationFailedException("Anonymous FTP is disabled");
      }
      if (!(authentication instanceof UsernamePasswordAuthentication passwordAuth)) {
        throw new AuthenticationFailedException("Unsupported FTP auth type");
      }
      OcppFtpServerProperties.Binding binding =
          byUsername.get(passwordAuth.getUsername());
      if (binding == null) {
        throw new AuthenticationFailedException("Unknown FTP user");
      }
      if (!binding.password().equals(passwordAuth.getPassword())) {
        throw new AuthenticationFailedException("Invalid FTP credentials");
      }
      return toUser(binding);
    }

    @Override
    public String getAdminName() {
      return null;
    }

    @Override
    public boolean isAdmin(String username) {
      return false;
    }

    private User toUser(OcppFtpServerProperties.Binding binding) {
      BaseUser user = new BaseUser();
      user.setName(binding.username());
      user.setPassword(binding.password());
      user.setEnabled(true);
      user.setAuthorities(java.util.List.of(new WritePermission(), new ConcurrentLoginPermission(0, 0)));
      user.setHomeDirectory(homeDirectory(binding).toString());
      return user;
    }

    private void prepareHome(OcppFtpServerProperties.Binding binding) {
      Path bindingRoot = homeDirectory(binding);
      try {
        Files.createDirectories(bindingRoot);
        for (String chargerId : binding.chargerIds()) {
          Files.createDirectories(bindingRoot.resolve(chargerId));
        }
      } catch (IOException e) {
        throw new IllegalStateException("Unable to initialize FTP storage for binding " + binding.id(), e);
      }
    }

    private Path homeDirectory(OcppFtpServerProperties.Binding binding) {
      return properties.rootDirectory().resolve("bindings").resolve(binding.id());
    }
  }
}
