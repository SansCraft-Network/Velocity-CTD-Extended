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
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.redis.VelocityRedis;
import com.velocitypowered.proxy.redis.depot.AbstractDepotService;
import com.velocitypowered.proxy.redis.impl.packet.VelocityKick;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents an extension of the {@link AbstractDepotService} for the player depot, including
 * functionality to track certain information about a single player, or multiple players.
 *
 * @author Elmar Blume - 18/05/2025
 */
public final class PlayerDepotService extends AbstractDepotService<UUID, PlayerEntry> {

  private final VelocityRedis redis;
  private final VelocityServer server;

  private final ScheduledTask updateTotalPlayerCountTask;
  private final ScheduledTask syncPlayerEntriesTask;

  private int totalPlayerCount = 0;

  /**
   * Constructs a new {@link PlayerDepotService}.
   *
   * @param redis the {@link VelocityRedis} instance
   */
  public PlayerDepotService(@NotNull VelocityRedis redis) {
    super(PlayerEntry.class, redis.getProvider());

    this.redis = redis;
    this.server = redis.getServer();

    // Start a task to update the total player count every 250 milliseconds
    this.updateTotalPlayerCountTask = redis.getServer().getScheduler()
            .buildTask(VelocityVirtualPlugin.INSTANCE, this::updateTotalPlayerCount)
            .repeat(Duration.ofMillis(250L))
            .schedule();

    // Start a task to update the player entries of this proxy every 1 second
    this.syncPlayerEntriesTask = redis.getServer().getScheduler()
            .buildTask(VelocityVirtualPlugin.INSTANCE, this::syncPlayerEntries)
            .repeat(Duration.ofSeconds(1L))
            .schedule();
  }

  @Override
  public void teardown() {
    // Remove all players of this proxy from the depot
    for (Player player : this.server.getAllPlayers()) {
      this.depot.remove(player.getUniqueId());
    }

    // Cancel scheduled tasks
    if (this.updateTotalPlayerCountTask != null) {
      this.updateTotalPlayerCountTask.cancel();
    }

    if (this.syncPlayerEntriesTask != null) {
      this.syncPlayerEntriesTask.cancel();
    }
  }

  /**
   * Called when a {@link ConnectedPlayer} connects to the proxy.
   *
   * @param player the player that connected
   * @return {@code true} if the player was successfully added to the depot, {@code false} otherwise
   */
  public boolean onPlayerConnect(final ConnectedPlayer player) {
    if (this.redis.isShutdown()) {
      return false;
    }

    // Check if the player is already connected
    if (this.depot.contains(player.getUniqueId())) {
      final Component component = Component.translatable("velocity.error.already-connected-proxy.remote");
      if (this.server.getConfiguration().isOnlineModeKickExistingPlayers()) {
        // Kick the existing player on any remote proxy
        new VelocityKick(player.getUniqueId(), component)
                .publish(); // allows the new connection
      } else {
        player.disconnect0(component, true);
        return false; // disallows the new connection
      }
    }

    this.upsertPlayerEntry(player);
    return true;
  }

  /**
   * Called when a {@link ConnectedPlayer} disconnects from the proxy.
   *
   * @param player the player that disconnected
   */
  public void onPlayerDisconnect(final ConnectedPlayer player) {
    if (this.redis.isShutdown()) {
      return;
    }

    this.depot.remove(player.getUniqueId());
  }

  /**
   * Called when a {@link ConnectedPlayer} switches servers.
   *
   * @param player the player that switched servers
   * @param serverName the name of the server that the player switched to
   */
  public void onPlayerSwitchServer(final ConnectedPlayer player, final String serverName) {
    final PlayerEntry playerEntry = this.getPlayerEntry(player.getUniqueId());
    if (playerEntry == null) {
      return;
    }

    playerEntry.setServerName(serverName);
    playerEntry.upsert();
  }

  /**
   * Get the total player count across all proxies, currently present in the depot.
   *
   * @return the total player count
   */
  public int getTotalPlayerCount() {
    return this.totalPlayerCount;
  }

  /**
   * Get a player entry by their unique ID.
   *
   * @param uniqueId the unique ID of the player
   * @return the player entry, or {@code null} if the player is not present in the depot
   */
  public @Nullable PlayerEntry getPlayerEntry(UUID uniqueId) {
    return this.depot.get(uniqueId);
  }

  /**
   * Get a player entry by their username.
   *
   * @param username the username of the player
   * @return the player entry, or {@code null} if the player is not present in the depot
   */
  public @Nullable PlayerEntry getPlayerEntry(String username) {
    for (PlayerEntry entry : this.depot.values()) {
      if (entry.getUsername().equalsIgnoreCase(username)) {
        return entry;
      }
    }
    return null;
  }

  /**
   * Checks whether a player is online.
   *
   * @param uniqueId the unique ID of the player
   * @return {@code true} if the player is online, {@code false} otherwise
   */
  public boolean isPlayerOnline(UUID uniqueId) {
    return this.depot.contains(uniqueId);
  }

  /**
   * Checks whether a player is online.
   *
   * @param username the username of the player
   * @return {@code true} if the player is online, {@code false} otherwise
   */
  public boolean isPlayerOnline(String username) {
    for (PlayerEntry entry : this.depot.values()) {
      if (entry.getUsername().equalsIgnoreCase(username)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Retrieves a list of player entries associated with a specific server.
   *
   * @param serverName the name of the server whose player entries are to be retrieved; must not be null
   * @return an unmodifiable list of {@link PlayerEntry} objects representing the players currently on the specified server; never null
   */
  public @NotNull @Unmodifiable List<PlayerEntry> getPlayerEntriesInServer(@NotNull String serverName) {
    return List.copyOf(this.queryAll(playerEntry -> serverName.equalsIgnoreCase(playerEntry.getServerName())));
  }

  /**
   * Retrieves a list of player entries associated with a specific proxy.
   *
   * @param proxyId the identifier of the proxy whose player entries are to be retrieved;
   *                must not be null or empty
   * @return an unmodifiable list of {@link PlayerEntry} objects representing players
   *         currently associated with the specified proxy; never null
   */
  public @NotNull @Unmodifiable List<PlayerEntry> getPlayerEntriesOnProxy(String proxyId) {
    return List.copyOf(this.queryAll(playerEntry -> playerEntry.getProxyId().equalsIgnoreCase(proxyId)));
  }

  /**
   * Upserts a player's entry in the depot. If an entry for the given player already exists,
   * it is updated with the latest details. If it doesn't exist, a new entry is created.
   *
   * @param player the {@link Player} object representing the player for whom the entry is to be upserted; must not be null
   * @return the {@link PlayerEntry} object representing the player's entry; never null
   */
  public @NotNull PlayerEntry upsertPlayerEntry(@NotNull Player player) {
    final PlayerEntry playerEntry = new PlayerEntry(player, this.redis.getProxyId());
    playerEntry.setDepot(this.depot);

    this.depot.upsert(playerEntry);
    return playerEntry;
  }

  /**
   * Updates the total player count by recalculating the number of entries in the depot.
   */
  private void updateTotalPlayerCount() {
    if (this.redis.isShutdown()) {
      return;
    }

    // depot size = amount of entries = total player count
    this.totalPlayerCount = this.depot.size();
  }

  /**
   * Synchronizes the player entries within the depot. This method ensures that the depot's
   * player entries are kept up to date and consistent with the current state of players on
   * the server.
   */
  private void syncPlayerEntries() {
    if (this.redis.isShutdown()) {
      return;
    }

    // Sync current players with depot
    for (Player player : this.server.getAllPlayers()) {
      if (this.depot.contains(player.getUniqueId())) {
        continue;
      }

      this.upsertPlayerEntry(player);
    }

    // Cleanup lost players
    for (PlayerEntry playerEntry : this.depot.values()) {
      if (!playerEntry.getProxyId().equalsIgnoreCase(this.redis.getProxyId())) {
        continue;
      }
      if (this.server.getPlayer(playerEntry.getUniqueId()).isPresent()) {
        continue;
      }

      playerEntry.remove();
    }
  }
}
