package com.velocitypowered.proxy.redis.impl.packet;

import com.velocitypowered.proxy.redis.packet.annotation.OneWayPacket;
import com.velocitypowered.proxy.redis.packet.typed.UUIDPacket;

import java.util.UUID;

/**
 * @author Elmar Blume - 04/10/2025
 */
@OneWayPacket
public final class VelocitySudo extends UUIDPacket {

  private final String message;

  public VelocitySudo(UUID playerUniqueId, String message) {
    super(playerUniqueId);
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
