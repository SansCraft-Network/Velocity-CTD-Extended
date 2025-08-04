package com.velocitypowered.proxy.xcd_redis.impl;

import com.velocitypowered.proxy.xcd_redis.packet.RedisPacket;
import com.velocitypowered.proxy.xcd_redis.packet.typed.ComponentPacket;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

/**
 * Represents a behaviour for a packet, which is used to define how the packet should behave,
 * for example, when an {@link com.velocitypowered.proxy.xcd_redis.transaction.Transaction} is about to be
 * completed, or when a {@link com.velocitypowered.proxy.xcd_redis.packet.annotation.OneWayPacket} is received
 *
 * @param <T> the type of packet
 * @param <C> the type of carrier
 * @author Elmar Blume - 18/05/2025
 */
public interface PacketBehaviour<T extends RedisPacket, C> {

  /**
   * Used whenever the Component payload is sent to any audience
   */
  PacketBehaviour<ComponentPacket, Audience> SEND_COMPONENT = (packet, carrier) -> {
    final Component component = packet.deserialize();
    if (component != null) {
      carrier.sendMessage(component);
    }
  };

  /**
   * Method used to let the desired packet 'behave', with a given carrier
   *
   * @param packet  the packet to have a behaviour
   * @param carrier the carrier used to support this behaviour
   */
  void behave(T packet, C carrier);

}
