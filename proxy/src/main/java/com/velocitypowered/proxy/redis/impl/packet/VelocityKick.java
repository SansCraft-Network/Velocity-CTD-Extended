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
import com.velocitypowered.proxy.redis.packet.typed.ComponentPacket;
import java.util.UUID;
import net.kyori.adventure.text.Component;

/**
 * Represents a packet that kicks a player from any proxy.
 *
 * @author Elmar Blume - 06/10/2025
 */
@OneWayPacket
public final class VelocityKick extends ComponentPacket {

  private final UUID uniqueId;

  /**
   * Constructs a new {@link VelocityKick} packet.
   *
   * @param uniqueId the player's unique ID
   * @param component the message to send
   */
  public VelocityKick(UUID uniqueId, Component component) {
    super(component);

    this.uniqueId = uniqueId;
  }

  /**
   * Gets the player's unique ID.
   *
   * @return the player's unique ID
   */
  public UUID getUniqueId() {
    return uniqueId;
  }
}
