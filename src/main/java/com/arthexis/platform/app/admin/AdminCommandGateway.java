package com.arthexis.platform.app.admin;

public interface AdminCommandGateway {
  AdminCommandResult submit(AdminCommandRequest request, String user);
}
