package com.velocitypowered.proxy.xcd_redis.impl;

import com.velocitypowered.proxy.xcd_redis.packet.RedisPacket;
import com.velocitypowered.proxy.xcd_redis.packet.typed.ComponentPacket;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.units.qual.C;

/**
 * Represents a set of default behaviours for a specific packet, which is used to define how the packet should behave,
 * for example, when an {@link com.velocitypowered.proxy.xcd_redis.transaction.Transaction} is about to be
 * completed, or when a {@link com.velocitypowered.proxy.xcd_redis.packet.annotation.OneWayPacket} is received.
 *
 * @param <C> the type of carrier
 * @param <T> the type of packet
 * @author Elmar Blume - 18/05/2025
 */
public interface PacketBehaviour<C, T extends RedisPacket> {

  /**
   * Used whenever the Component payload is sent to any audience
   */
  PacketBehaviour<Audience, ComponentPacket> SEND_COMPONENT = (carrier, packet) -> {
    final Component component = packet.deserialize();
    if (component != null) {
      carrier.sendMessage(component);
    }
  };

  /**
   * Method used to let the desired packet 'behave', with a given carrier
   *
   * @param carrier the carrier used to support this behaviour
   * @param packet  the packet to have a behaviour
   */
  void behave(C carrier, T packet);

}
