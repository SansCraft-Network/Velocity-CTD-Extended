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

package com.velocitypowered.proxy.redis.packet;

/**
 * Represents a packet that does not have a specific type set yet.
 *
 * @author Elmar Blume - 08/05/2025
 */
public non-sealed class GenericPacket<T> extends AbstractRedisPacket {

  protected final T payload;

  /**
   * Constructs a new {@link GenericPacket}.
   *
   * @param payload the payload of the packet
   */
  public GenericPacket(T payload) {
    super();
    this.payload = payload;
  }

  /**
   * Get the payload of the packet.
   *
   * @return the payload of the packet
   */
  public T getPayload() {
    return payload;
  }

}
