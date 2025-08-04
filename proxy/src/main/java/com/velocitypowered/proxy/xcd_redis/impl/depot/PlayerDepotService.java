package com.velocitypowered.proxy.xcd_redis.impl.depot;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.xcd_redis.VelocityRedis;
import com.velocitypowered.proxy.xcd_redis.depot.AbstractDepotService;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
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

    // Start a task to update the total player count every 100 milliseconds
    redis.getServer().getScheduler().buildTask(VelocityVirtualPlugin.INSTANCE, this::updateTotalPlayerCount)
            .repeat(Duration.ofMillis(100L)).schedule();

    // Start a task to update the player entries of this proxy every 1 second
    redis.getServer().getScheduler().buildTask(VelocityVirtualPlugin.INSTANCE, this::syncPlayerEntries)
            .repeat(Duration.ofSeconds(1L)).schedule();
  }

  /**
   * Get the total player count across all proxies, currently present in the depot
   *
   * @return the total player count
   */
  public int getTotalPlayerCount() {
    return this.totalPlayerCount;
  }

  private void updateTotalPlayerCount() {
    // depot size = amount of entries = total player count
    this.totalPlayerCount = this.depot.size();
  }

  private void syncPlayerEntries() {
    // Sync current players with depot
    for (Player player : this.server.getAllPlayers()) {
      if (this.depot.contains(player.getUniqueId())) continue;

      final PlayerEntry playerEntry = new PlayerEntry(
              player.getUniqueId(),
              player.getUsername(),
              this.redis.getProxyId(),
              Map.of(), false, false); //todo sync with queue system
      playerEntry.setServer(player.getCurrentServer().orElse(null));

      this.depot.upsert(playerEntry);
    }

    // Cleanup lost players
    for (PlayerEntry playerEntry : this.depot.values()) {
      if (!playerEntry.getProxyId().equalsIgnoreCase(this.redis.getProxyId())) continue;
      if (this.server.getPlayer(playerEntry.getUniqueId()).isPresent()) continue;

      playerEntry.remove();
    }
  }
}
