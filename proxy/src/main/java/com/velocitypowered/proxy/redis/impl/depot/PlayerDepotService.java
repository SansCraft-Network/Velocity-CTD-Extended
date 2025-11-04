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
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.redis.VelocityRedis;
import com.velocitypowered.proxy.redis.depot.AbstractDepotService;
import com.velocitypowered.proxy.redis.impl.packet.VelocityKick;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Represents an extension of the {@link AbstractDepotService} for the player depot, including
 * functionality to track certain information about a single player, or multiple players.
 *
 * @author Elmar Blume - 18/05/2025
 */
public final class PlayerDepotService extends AbstractDepotService<UUID, PlayerEntry> {

  private final VelocityRedis redis;
  private final VelocityServer server;

  private int totalPlayerCount = 0;

  /**
   * Constructs a new {@link PlayerDepotService}
   *
   * @param redis the {@link VelocityRedis} instance
   */
  public PlayerDepotService(@NotNull VelocityRedis redis) {
    super(PlayerEntry.class, redis.getProvider());

    this.redis = redis;
    this.server = redis.getServer();

    // Start a task to update the total player count every 250 milliseconds
    redis.getServer().getScheduler().buildTask(VelocityVirtualPlugin.INSTANCE, this::updateTotalPlayerCount)
            .repeat(Duration.ofMillis(250L)).schedule();//todo stop at teardown

    // Start a task to update the player entries of this proxy every 1 second
    redis.getServer().getScheduler().buildTask(VelocityVirtualPlugin.INSTANCE, this::syncPlayerEntries)
            .repeat(Duration.ofSeconds(1L)).schedule();//todo stop at teardown
  }

  @Override
  public void teardown() {
    // Remove all players of this proxy from the depot
    for (Player player : this.server.getAllPlayers()) {
      this.depot.remove(player.getUniqueId());
    }
  }

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

  public void onPlayerDisconnect(final ConnectedPlayer player) {
    if (this.redis.isShutdown()) {
      return;
    }

    this.depot.remove(player.getUniqueId());
  }

  public void onPlayerSwitchServer(final ConnectedPlayer player, final String serverName) {
    final PlayerEntry playerEntry = this.getPlayerEntry(player.getUniqueId());
    if (playerEntry == null) {
      return;
    }

    playerEntry.setServerName(serverName);
    playerEntry.upsert();
  }

  /**
   * Get the total player count across all proxies, currently present in the depot
   *
   * @return the total player count
   */
  public int getTotalPlayerCount() {
    return this.totalPlayerCount;
  }

  public @Nullable PlayerEntry getPlayerEntry(UUID uniqueId) {
    return this.depot.get(uniqueId);
  }

  public @Nullable PlayerEntry getPlayerEntry(String username) {
    for (PlayerEntry entry : this.depot.values()) {
      if (entry.getUsername().equalsIgnoreCase(username)) {
        return entry;
      }
    }
    return null;
  }

  public boolean isPlayerOnline(UUID uniqueId) {
    return this.depot.contains(uniqueId);
  }

  public boolean isPlayerOnline(String username) {
    for (PlayerEntry entry : this.depot.values()) {
      if (entry.getUsername().equalsIgnoreCase(username)) {
        return true;
      }
    }
    return false;
  }

  public @NotNull @Unmodifiable List<PlayerEntry> getPlayerEntriesInServer(@NotNull String serverName) {
    return List.copyOf(this.queryAll(playerEntry -> serverName.equalsIgnoreCase(playerEntry.getServerName())));
  }

  public @NotNull @Unmodifiable List<PlayerEntry> getPlayerEntriesOnProxy(String proxyId) {
    return List.copyOf(this.queryAll(playerEntry -> playerEntry.getProxyId().equalsIgnoreCase(proxyId)));
  }

  public @NotNull PlayerEntry upsertPlayerEntry(@NotNull Player player) {
    final PlayerEntry playerEntry = new PlayerEntry(player, this.redis.getProxyId());
    playerEntry.setDepot(this.depot);

    this.depot.upsert(playerEntry);
    return playerEntry;
  }

  private void updateTotalPlayerCount() {
    if (this.redis.isShutdown()) {
      return;
    }

    // depot size = amount of entries = total player count
    this.totalPlayerCount = this.depot.size();
  }

  private void syncPlayerEntries() {
    if (this.redis.isShutdown()) {
      return;
    }

    // Sync current players with depot
    for (Player player : this.server.getAllPlayers()) {
      if (this.depot.contains(player.getUniqueId())) continue;

      this.upsertPlayerEntry(player);
    }

    // Cleanup lost players
    for (PlayerEntry playerEntry : this.depot.values()) {
      if (!playerEntry.getProxyId().equalsIgnoreCase(this.redis.getProxyId())) continue;
      if (this.server.getPlayer(playerEntry.getUniqueId()).isPresent()) continue;

      playerEntry.remove();
    }
  }
}
