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

package com.velocitypowered.proxy.redis.impl.depot;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.proxy.redis.depot.DepotEntry;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elmar Blume - 18/05/2025
 */
public final class PlayerEntry extends DepotEntry<UUID, PlayerEntry> {

  private final String username;
  private final String proxyId;

  private String serverName = null;

  public PlayerEntry(UUID uniqueId, String username, String proxyId) {
    super(uniqueId);

    this.username = username;
    this.proxyId = proxyId;
  }

  public PlayerEntry(final @NotNull Player player, String proxyId) {
    this(player.getUniqueId(), player.getUsername(), proxyId);
    this.setServer(player.getCurrentServer().orElse(null));
  }

  public void setServer(@Nullable ServerConnection connection) {
    if (connection == null) {
      this.serverName = null;
      return;
    }

    this.serverName = connection.getServerInfo().getName();
  }

  public String getUsername() {
    return username;
  }

  public String getProxyId() {
    return proxyId;
  }

  public String getServerName() {
    return serverName;
  }

  public void setServerName(String serverName) {
    this.serverName = serverName;
  }
}
