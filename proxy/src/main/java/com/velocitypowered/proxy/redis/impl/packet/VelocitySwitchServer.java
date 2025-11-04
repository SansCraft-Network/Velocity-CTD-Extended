package com.velocitypowered.proxy.redis.impl.packet;

import com.velocitypowered.proxy.redis.packet.annotation.OneWayPacket;
import com.velocitypowered.proxy.redis.packet.typed.StringPacket;

/**
 * @author Elmar Blume - 20/06/2025
 */
@OneWayPacket
public final class VelocitySwitchServer extends StringPacket {

  private final String username;
  private final String serverName;

  public VelocitySwitchServer(String username, String serverName) {
    super(username);

    this.username = username;
    this.serverName = serverName;
  }

  public String getUsername() {
    return username;
  }

  public String getServerName() {
    return serverName;
  }
}
