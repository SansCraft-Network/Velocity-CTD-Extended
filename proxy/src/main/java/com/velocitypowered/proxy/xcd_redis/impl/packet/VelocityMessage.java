package com.velocitypowered.proxy.xcd_redis.impl.packet;

import com.velocitypowered.proxy.xcd_redis.impl.model.EncodedCommandSource;
import com.velocitypowered.proxy.xcd_redis.packet.annotation.OneWayPacket;
import com.velocitypowered.proxy.xcd_redis.packet.typed.ComponentPacket;
import net.kyori.adventure.text.Component;

/**
 * @author Elmar Blume - 09/05/2025
 */
@OneWayPacket
public final class VelocityMessage extends ComponentPacket {

  private final EncodedCommandSource target;

  public VelocityMessage(EncodedCommandSource target, Component component) {
    super(component);
    this.target = target;
  }

  public EncodedCommandSource getTarget() {
    return target;
  }
}
