/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

package com.velocityctd.proxy.redis.impl.packet;

import com.velocityctd.proxy.redis.packet.annotation.OneWayPacket;
import com.velocityctd.proxy.redis.packet.typed.ComponentPacket;
import java.util.UUID;
import net.kyori.adventure.text.Component;

/**
 * Represents a packet used to remotely kick a player from another proxy.
 *
 * <p>This packet transports both the unique player identifier and the
 * disconnect message, and is handled as a one-way message across proxies.</p>
 */
@OneWayPacket
public final class VelocityKick extends ComponentPacket {

  /**
   * The unique identifier of the player being kicked.
   */
  private final UUID uniqueId;

  /**
   * Constructs a new {@link VelocityKick} packet.
   *
   * @param uniqueId the player's unique ID
   * @param component the message to send
   */
  public VelocityKick(final UUID uniqueId, final Component component) {
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
