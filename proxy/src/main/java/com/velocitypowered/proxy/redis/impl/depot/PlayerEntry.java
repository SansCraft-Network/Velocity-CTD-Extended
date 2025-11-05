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
 * Represents a player entry in the depot.
 *
 * @author Elmar Blume - 18/05/2025
 */
public final class PlayerEntry extends DepotEntry<UUID, PlayerEntry> {

  private final String username;
  private final String proxyId;

  private String serverName = null;

  /**
   * Constructs a new {@link PlayerEntry}.
   *
   * @param uniqueId the player's unique id
   * @param username the player's username
   * @param proxyId the ID of the proxy the player is on
   */
  public PlayerEntry(UUID uniqueId, String username, String proxyId) {
    super(uniqueId);

    this.username = username;
    this.proxyId = proxyId;
  }

  /**
   * Constructs a new {@link PlayerEntry} from a {@link Player}.
   *
   * @param player the player to construct from
   * @param proxyId the ID of the proxy the player is on
   */
  public PlayerEntry(final @NotNull Player player, String proxyId) {
    this(player.getUniqueId(), player.getUsername(), proxyId);
    this.setServer(player.getCurrentServer().orElse(null));
  }

  /**
   * Sets the server the player is currently on.
   *
   * @param connection the server connection the player is on
   */
  public void setServer(@Nullable ServerConnection connection) {
    if (connection == null) {
      this.serverName = null;
      return;
    }

    this.serverName = connection.getServerInfo().getName();
  }

  /**
   * Gets the username.
   *
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Gets the proxy ID.
   *
   * @return the proxy ID
   */

  public String getProxyId() {
    return proxyId;
  }

  /**
   * Gets the server name.
   *
   * @return the server name
   */
  public String getServerName() {
    return serverName;
  }

  /**
   * Sets the server name.
   *
   * @param serverName the server name
   */
  public void setServerName(String serverName) {
    this.serverName = serverName;
  }
}
