package com.velocitypowered.proxy.redis.impl.packet;

import com.velocitypowered.proxy.redis.packet.annotation.OneWayPacket;
import com.velocitypowered.proxy.redis.packet.typed.ComponentPacket;
import net.kyori.adventure.text.Component;

import java.util.UUID;

/**
 * @author Elmar Blume - 06/10/2025
 */
@OneWayPacket
public final class VelocityKick extends ComponentPacket {

  private final UUID uniqueId;

  public VelocityKick(UUID uniqueId, Component component) {
    super(component);

    this.uniqueId = uniqueId;
  }

  public UUID getUniqueId() {
    return uniqueId;
  }
}
