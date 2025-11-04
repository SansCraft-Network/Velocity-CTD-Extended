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
