package com.velocitypowered.proxy.queue.model;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.queue.Queue;
import com.velocitypowered.proxy.redis.impl.depot.PlayerEntry;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the data of a player in a queue, used to construct a {@link QueuePlayer}.
 *
 * @author Elmar Blume - 06/11/2025
 */
public record QueuePlayerData(@NotNull UUID uniqueId, @NotNull String username, int priority, boolean fullBypass, boolean queueBypass) {

  /**
   * Constructs a new {@link QueuePlayerData} instance.
   *
   * @param uniqueId    the unique identifier of the player
   * @param username    the username of the player
   * @param priority    the queue priority of the player
   * @param fullBypass  whether the player bypasses full server limits
   * @param queueBypass whether the player bypasses the queue entirely
   */
  public QueuePlayerData {
    if (priority < 0) {
      throw new IllegalArgumentException("Priority cannot be negative");
    }
  }

  /**
   * Creates a {@link QueuePlayerData} instance from a {@link Player} and a {@link Queue}.
   *
   * @param queue  the queue the player is joining
   * @param player the player joining the queue
   * @return a new {@link QueuePlayerData} instance
   */
  public static @NotNull QueuePlayerData of(final @NotNull Queue queue, final @NotNull Player player) {
    return new QueuePlayerData(
        player.getUniqueId(),
        player.getUsername(),
        player.getQueuePriority(queue.getName()),
        player.hasPermission("velocity.queue.full.bypass"),
        player.hasPermission("velocity.queue.bypass")
    );
  }

  /**
   * Creates a {@link QueuePlayerData} instance from a {@link PlayerEntry} and a {@link Queue}.
   *
   * @param queue       the queue the player is joining
   * @param playerEntry the player entry joining the queue
   * @return a new {@link QueuePlayerData} instance
   */
  public static @NotNull QueuePlayerData of(final @NotNull Queue queue, final @NotNull PlayerEntry playerEntry) {
    return new QueuePlayerData(
        playerEntry.getUniqueId(),
        playerEntry.getUsername(),
        playerEntry.getQueuePriorities().getOrDefault(queue.getName(), 0),
        playerEntry.isFullQueueBypass(),
        playerEntry.isQueueBypass()
    );
  }
}
