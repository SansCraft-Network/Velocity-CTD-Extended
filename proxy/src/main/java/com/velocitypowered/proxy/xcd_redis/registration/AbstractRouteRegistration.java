package com.velocitypowered.proxy.xcd_redis.registration;

import com.velocitypowered.proxy.xcd_redis.packet.RedisPacket;

/**
 * @author Elmar Blume - 08/05/2025
 */
public abstract sealed class AbstractRouteRegistration<T extends RedisPacket> implements RouteRegistration<T>
        permits ConsumerRouteRegistration {

  private final Class<T> packetClass;

  /**
   * Constructs a new {@link AbstractRouteRegistration}
   *
   * @param packetClass the class type of the {@link RedisPacket}
   */
  public AbstractRouteRegistration(Class<T> packetClass) {
    this.packetClass = packetClass;
  }

  @Override
  public Class<T> getPacketClass() {
    return packetClass;
  }
}
