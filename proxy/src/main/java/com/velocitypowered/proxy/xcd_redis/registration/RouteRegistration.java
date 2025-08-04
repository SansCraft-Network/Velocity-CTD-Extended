package com.velocitypowered.proxy.xcd_redis.registration;

import com.velocitypowered.proxy.xcd_redis.packet.RedisPacket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Represents a route registration for a {@link RedisPacket}, implemented through
 * the {@link AbstractRouteRegistration} class.
 *
 * @author Elmar Blume - 08/05/2025
 */
public sealed interface RouteRegistration<T extends RedisPacket> permits AbstractRouteRegistration {

  /**
   * Creates a new {@link ConsumerRouteRegistration} for the given {@link RedisPacket} class and consumer
   *
   * @param packetClass the class type of the packet
   * @param consumer    the consumer to handle the packet
   * @param <T>         the type of the packet
   * @return a new consumer route registration instance
   */
  @Contract("_, _ -> new")
  static <T extends RedisPacket> @NotNull ConsumerRouteRegistration<T> consumer(Class<T> packetClass, Consumer<T> consumer) {
    return new ConsumerRouteRegistration<>(packetClass, consumer);
  }

  /**
   * Get the class type of the {@link RedisPacket}
   *
   * @return the class type of the packet
   */
  Class<T> getPacketClass();

}
