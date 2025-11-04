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

package com.velocitypowered.proxy.queue;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.translation.Argument;

/**
 * @author Elmar Blume - 29/10/2025
 */
public final class MemoryQueue extends AbstractQueue {

  public MemoryQueue(VelocityServer server,  VelocityRegisteredServer backendInstance) {
    super(server, backendInstance);
  }

  @Override
  protected void notifyMaxRetriesReached(Player player) {
    final TranslatableComponent component = Component.translatable("velocity.queue.error.max-send-retries-reached")
            .arguments(Argument.string("server", this.getName()),
                    Argument.numeric("retries", this.server.getConfiguration().getQueue().getMaxSendRetries()));

    player.sendMessage(component);
  }
}
