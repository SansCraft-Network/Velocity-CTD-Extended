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

package com.velocitypowered.proxy.queue.cache;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.queue.MemoryQueue;
import com.velocitypowered.proxy.queue.Queue;
import com.velocitypowered.proxy.queue.exception.QueueCacheException;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the in-memory cache implementation of {@link QueueCache} for a {@link Queue}
 *
 * @author Elmar Blume - 03/04/2025
 */
public final class MemoryQueueCache implements QueueCache {

  private final VelocityServer server;
  private final ConcurrentHashMap<String, Queue> queues;

  /**
   * Constructs a new {@link MemoryQueueCache}
   *
   * @param server the proxy instance
   */
  public MemoryQueueCache(VelocityServer server) {
    this.server = server;
    this.queues = new ConcurrentHashMap<>();
  }

  @Override
  public @NotNull Queue getQueue(@NotNull String serverName) {
    final VelocityRegisteredServer backendInstance = (VelocityRegisteredServer) this.server.getServer(serverName)
            .orElseThrow(() -> new QueueCacheException(serverName));
    return this.queues.computeIfAbsent(serverName, $ -> new MemoryQueue(server, backendInstance));
  }

  @Override
  public Queue getQueue(@NotNull Player player) {
    for (Queue queue : getQueues()) {
      if (queue.contains(player)) {
        return queue;
      }
    }

    return null;
  }

  @Override
  public Collection<Queue> getQueues() {
    return List.copyOf(this.queues.values());
  }

  @Override
  public void updateQueue(@NotNull Queue queue) {
    this.queues.put(queue.getName(), queue);
  }
}


