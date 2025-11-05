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
import org.jetbrains.annotations.NotNull;

/**
 * Represents a packet that sends an action bar message to a player.
 *
 * @author Elmar Blume - 09/05/2025
 */
@OneWayPacket
public final class VelocityActionBar extends ComponentPacket {

  private final UUID uniqueId;

  /**
   * Constructs a new {@link VelocityActionBar} packet.
   *
   * @param uniqueId the uniqueId if the player to send the message to.
   * @param component the message to send.
   */
  public VelocityActionBar(final @NotNull UUID uniqueId, final Component component) {
    super(component);
    this.uniqueId = uniqueId;
  }

  /**
   * Gets the unique identifier of the player to send the message to.
   *
   * @return the unique identifier of the player
   */
  public UUID getUniqueId() {
    return uniqueId;
  }
}
