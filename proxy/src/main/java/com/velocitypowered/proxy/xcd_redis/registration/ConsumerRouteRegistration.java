package com.velocitypowered.proxy.xcd_redis.registration;

import com.velocitypowered.proxy.xcd_redis.packet.RedisPacket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Represents a route registration for a {@link RedisPacket}, implemented through
 * the {@link AbstractRouteRegistration} class utilizing a {@link Consumer} to handle the packet.
 *
 * @author Elmar Blume - 08/05/2025
 */
public non-sealed class ConsumerRouteRegistration<T extends RedisPacket> extends AbstractRouteRegistration<T> {

  private final Consumer<T> consumer;

  /**
   * Constructs a new {@link ConsumerRouteRegistration}
   *
   * @param packetClass the class type of the packet
   * @param consumer   the consumer to handle the packet
   */
  public ConsumerRouteRegistration(Class<T> packetClass, Consumer<T> consumer) {
    super(packetClass);

    this.consumer = consumer;
  }

  /**
   * Get the consumer of the route registration
   *
   * @return the consumer of the route registration
   */
  public Consumer<T> getConsumer() {
    return consumer;
  }
}
