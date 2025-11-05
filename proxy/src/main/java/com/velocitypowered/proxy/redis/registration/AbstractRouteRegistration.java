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

/**
 * Represents an abstract route registration for a {@link RedisPacket}.
 *
 * @author Elmar Blume - 08/05/2025
 */
public abstract sealed class AbstractRouteRegistration<T extends RedisPacket> implements RouteRegistration<T>
        permits ConsumerRouteRegistration {

  private final Class<T> packetClass;

  /**
   * Constructs a new {@link AbstractRouteRegistration}.
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
