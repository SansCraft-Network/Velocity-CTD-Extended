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

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.redis.impl.model.EncodedCommandSource;
import com.velocitypowered.proxy.redis.packet.annotation.OneWayPacket;
import com.velocitypowered.proxy.redis.packet.typed.ComponentPacket;
import java.util.UUID;
import net.kyori.adventure.text.Component;

/**
 * @author Elmar Blume - 09/05/2025
 */
@OneWayPacket
public final class VelocityMessage extends ComponentPacket {

  private EncodedCommandSource commandSource = null;
  private UUID playerUniqueId = null;

  public VelocityMessage(EncodedCommandSource commandSource, Component component) {
    super(component);
    this.commandSource = commandSource;
  }

  public VelocityMessage(UUID playerUniqueId, Component component) {
    super(component);
    this.playerUniqueId = playerUniqueId;
  }

  public void sendMessage(final VelocityServer server) {
    final Component component = this.deserialize();
    if (component == null) {
      return;
    }

    if (this.playerUniqueId != null) {
      server.getPlayer(this.playerUniqueId).ifPresent(player -> {
        player.sendMessage(component);
      });
    } else if (this.commandSource != null) {
      this.commandSource.sendMessage(server, component);
    }
  }
}
