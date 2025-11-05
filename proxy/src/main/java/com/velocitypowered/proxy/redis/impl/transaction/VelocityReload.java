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

package com.velocitypowered.proxy.redis.impl.transaction;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.redis.impl.PacketBehaviour;
import com.velocitypowered.proxy.redis.packet.typed.ComponentPacket;
import com.velocitypowered.proxy.redis.packet.typed.StringPacket;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a transaction that reloads any proxy.
 *
 * @author Elmar Blume - 02/10/2025
 */
public final class VelocityReload extends VelocityTransaction<StringPacket, ComponentPacket> {

  /**
   * Constructs a new {@link VelocityReload} transaction.
   *
   * @param source the command source to send the result to
   * @param proxyId the id of the proxy to reload
   */
  public VelocityReload(@NotNull CommandSource source, @NotNull String proxyId) {
    super(new StringPacket(proxyId), source, "xcd_redis.command.reload.timeout");

    // Send the uptime result to the command source
    this.onComplete(packet -> PacketBehaviour.SEND_COMPONENT.behave(source, packet));
  }
}
