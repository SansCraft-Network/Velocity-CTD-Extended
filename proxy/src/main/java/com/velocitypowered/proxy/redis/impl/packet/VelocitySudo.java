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

package com.velocitypowered.proxy.redis.impl.packet;

import com.velocitypowered.proxy.redis.packet.annotation.OneWayPacket;
import com.velocitypowered.proxy.redis.packet.typed.UUIDPacket;

import java.util.UUID;

/**
 * @author Elmar Blume - 04/10/2025
 */
@OneWayPacket
public final class VelocitySudo extends UUIDPacket {

  private final String message;

  public VelocitySudo(UUID playerUniqueId, String message) {
    super(playerUniqueId);
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
