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
import com.velocitypowered.proxy.redis.packet.RedisPacket;
import com.velocitypowered.proxy.redis.transaction.Transaction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elmar Blume - 02/10/2025
 */
public abstract class VelocityTransaction<T extends RedisPacket, R extends RedisPacket> extends Transaction<T, R> {

  public VelocityTransaction(@NotNull T sentPacket, @Nullable CommandSource source, @Nullable String timeoutTranslationKey) {
    super(sentPacket);

    // Set the default timeout behavior to send a message to the command source
    if (source != null && timeoutTranslationKey != null)
      this.onTimeout(t -> source.sendMessage(Component.text(timeoutTranslationKey, NamedTextColor.RED)));
  }

  public VelocityTransaction(@NotNull T sentPacket) {
    this(sentPacket, null, null);
  }
}
