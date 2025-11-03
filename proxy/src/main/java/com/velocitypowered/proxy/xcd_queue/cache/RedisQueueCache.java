package com.velocitypowered.proxy.xcd_queue.cache;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.xcd_queue.Queue;
import com.velocitypowered.proxy.xcd_queue.RedisQueue;
import com.velocitypowered.proxy.xcd_queue.depot.QueueDepotService;
import com.velocitypowered.proxy.xcd_queue.depot.QueueEntry;
import com.velocitypowered.proxy.xcd_queue.exception.QueueCacheException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Elmar Blume - 03/04/2025
 */
public final class RedisQueueCache implements QueueCache {

  private final VelocityServer server;
  private final QueueDepotService service;

  public RedisQueueCache(final @NotNull VelocityServer server) {
    this.server = server;
    this.service = server.getRedis().getQueueService();
  }

  @Override
  public Queue getQueue(@NotNull String serverName) {
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
