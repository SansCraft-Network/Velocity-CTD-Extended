/*
 * Copyright (C) 2025 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.redis.registration;

import com.velocitypowered.proxy.redis.packet.RedisPacket;
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
