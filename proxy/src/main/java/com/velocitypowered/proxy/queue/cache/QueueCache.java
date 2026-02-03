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

package com.velocitypowered.proxy.queue.cache;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.queue.Queue;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a cache that stores the queue data, either in memory or in a Redis database.
 *
 * @see MemoryQueueCache
 * @see RedisQueueCache
 */
public sealed interface QueueCache permits MemoryQueueCache, RedisQueueCache {

  /**
   * Retrieves the queue for the given server name. This will create a new queue if it doesn't exist.
   *
   * @param serverName the server name to retrieve the queue for
   * @return the queue for the given server name, or a new queue if it doesn't exist
   */
  @NotNull
  Queue getQueue(@NotNull String serverName);

  /**
   * Retrieves the queue for the given player.
   *
   * @param player the player to retrieve the queue for
   * @return the queue for the given player, or {@code null} if the player is not queued
   */
  @Nullable
  Queue getQueue(@NotNull Player player);

  /**
   * Retrieves all queues stored in the cache.
   *
   * @return a collection of all queues in the cache
   */
  Collection<Queue> getQueues();

  /**
   * Updates the queue in the cache.
   *
   * @param queue the queue to update
   */
  void updateQueue(@NotNull Queue queue);
}
