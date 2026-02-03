/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

package com.velocitypowered.proxy.queue.model;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.queue.Queue;
import com.velocitypowered.proxy.redis.impl.depot.PlayerEntry;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the data of a player in a queue, used to construct a {@link QueuePlayer}.
 *
 * @param uniqueId    the unique identifier of the player
 * @param username    the username of the player
 * @param priority    the queue priority of the player
 * @param fullBypass  whether the player bypasses full server limits
 * @param queueBypass whether the player bypasses the queue entirely
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
