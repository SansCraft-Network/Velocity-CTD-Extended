/*
 * Copyright (C) 2018-2025 Velocity Contributors
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a player entry in the depot.
 */
public final class PlayerEntry extends DepotEntry<UUID, PlayerEntry> {

  /**
   * The username of the player represented by this entry.
   */
  private final String username;

  /**
   * The identifier of the proxy on which the player is currently connected.
   */
  private final String proxyId;

  /**
   * A map of queue priorities assigned to this player,
   * indexed by queue/server name.
   */
  private final Map<String, Integer> queuePriority;

  /**
   * Whether this player is permitted to bypass full queue restrictions.
   */
  private boolean fullQueueBypass = false;

  /**
   * Whether this player is permitted to bypass queue placement entirely.
   */
  private boolean queueBypass = false;

  /**
   * The name of the backend server the player is currently connected to,
   * or {@code null} if the player is not on any server.
   */
  private String serverName = null;

  /**
   * Constructs a new {@link PlayerEntry}.
   *
   * @param uniqueId the player's unique id
   * @param username the player's username
   * @param proxyId the ID of the proxy the player is on
   */
  public PlayerEntry(final UUID uniqueId, final String username, final String proxyId) {
    super(uniqueId);

    this.username = username;
    this.proxyId = proxyId;
    this.queuePriority = new HashMap<>();
  }

  /**
   * Constructs a new {@link PlayerEntry} from a {@link Player}.
   *
   * @param player the player to construct from
   * @param proxyId the ID of the proxy the player is on
   */
  public PlayerEntry(final @NotNull Player player, final String proxyId) {
    this(player.getUniqueId(), player.getUsername(), proxyId);

    this.setServer(player.getCurrentServer().orElse(null));
    this.fullQueueBypass = player.hasPermission("velocity.queue.full.bypass");
    this.queueBypass = player.hasPermission("velocity.queue.bypass");
    this.queuePriority.putAll(player.getQueuePriorities());
  }

  /**
   * Sets the server the player is currently on.
   *
   * @param connection the server connection the player is on
   */
  public void setServer(final @Nullable ServerConnection connection) {
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
   * Gets the queue priorities.
   *
   * @return the queue priorities
   */
  public Map<String, Integer> getQueuePriorities() {
    return queuePriority;
  }

  /**
   * Checks whether the player bypasses full queues.
   *
   * @return {@code true} if the player bypasses full queues, {@code false} otherwise
   */
  public boolean isFullQueueBypass() {
    return fullQueueBypass;
  }

  /**
   * Gets the queue bypass mode.
   *
   * @return the queue bypass mode
   */
  public boolean isQueueBypass() {
    return queueBypass;
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
  public void setServerName(final String serverName) {
    this.serverName = serverName;
  }
}
