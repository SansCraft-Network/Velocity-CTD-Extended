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
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.queue.Queue;
import com.velocitypowered.proxy.queue.RedisQueue;
import com.velocitypowered.proxy.queue.exception.QueueCacheException;
import com.velocitypowered.proxy.queue.redis.depot.QueueDepotService;
import com.velocitypowered.proxy.queue.redis.depot.QueueEntry;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the redis cache implementation of {@link QueueCache} for a {@link Queue}
 *
 * @author Elmar Blume - 03/04/2025
 */
public final class RedisQueueCache implements QueueCache {

  private final VelocityServer server;
  private final QueueDepotService service;

  /**
   * Constructs a new {@link RedisQueueCache}
   *
   * @param server the proxy instance
   */
  public RedisQueueCache(final @NotNull VelocityServer server) {
    this.server = server;
    this.service = server.getRedis().getQueueService();
  }

  @Override
  public @NotNull Queue getQueue(@NotNull String serverName) {
    final VelocityRegisteredServer backendInstance = (VelocityRegisteredServer) this.server.getServer(serverName)
            .orElseThrow(() -> new QueueCacheException(serverName));

    final QueueEntry queueEntry = this.service.getQueueEntry(serverName);
    final RedisQueue redisQueue = queueEntry == null
            ? new RedisQueue(server, backendInstance)
            : new RedisQueue(server, backendInstance, queueEntry);

    CompletableFuture.runAsync(() -> this.updateQueue(redisQueue));
    return redisQueue;
  }

  @Override
  public @Nullable Queue getQueue(@NotNull Player player) {
    for (Queue queue : getQueues()) {
      if (queue.contains(player)) {
        return queue;
      }
    }

    return null;
  }

  @Override
  public Collection<Queue> getQueues() {
    final List<QueueEntry> entries = this.service.getAll();
    final List<Queue> queues = new ArrayList<>();

    if (entries.isEmpty()) {
      for (RegisteredServer registeredServer : this.server.getAllServers()) {
        final VelocityRegisteredServer backendInstance = (VelocityRegisteredServer) registeredServer;

        final RedisQueue redisQueue = new RedisQueue(server, backendInstance);
        CompletableFuture.runAsync(() -> this.updateQueue(redisQueue));

        queues.add(redisQueue);
      }
    } else {
      for (QueueEntry entry : entries) {
        final VelocityRegisteredServer backendInstance = (VelocityRegisteredServer) this.server
                .getServer(entry.getUniqueId()).orElse(null);

        if (backendInstance != null) {
          queues.add(new RedisQueue(server, backendInstance, entry));
        }
      }
    }

    return queues;
  }

  @Override
  public void updateQueue(@NotNull Queue queue) {
    this.service.insertQueueEntry(queue);
  }
}
