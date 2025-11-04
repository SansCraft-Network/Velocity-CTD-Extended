package com.velocitypowered.proxy.redis.impl.packet;

import com.velocitypowered.proxy.redis.packet.typed.UUIDPacket;

import java.util.UUID;

/**
 * @author Elmar Blume - 06/10/2025
 */
public final class VelocityRemote extends UUIDPacket {

  private final String proxyId;
  private final String ip;
  private final int port;

  public VelocityRemote(UUID payload, String proxyId, String ip, int port) {
    super(payload);

    this.proxyId = proxyId;
    this.ip = ip;
    this.port = port;
  }

  public String getProxyId() {
    return proxyId;
  }

  public String getIp() {
    return ip;
  }

  public int getPort() {
    return port;
  }
}
