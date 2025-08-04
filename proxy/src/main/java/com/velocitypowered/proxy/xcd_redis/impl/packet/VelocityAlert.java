package com.velocitypowered.proxy.xcd_redis.impl.packet;

import com.velocitypowered.proxy.xcd_redis.packet.annotation.OneWayPacket;
import com.velocitypowered.proxy.xcd_redis.packet.typed.ComponentPacket;
import net.kyori.adventure.text.Component;

/**
 * @author Elmar Blume - 09/05/2025
 */
@OneWayPacket
public final class VelocityAlert extends ComponentPacket {

  public VelocityAlert(Component component) {
    super(component);
  }

}
