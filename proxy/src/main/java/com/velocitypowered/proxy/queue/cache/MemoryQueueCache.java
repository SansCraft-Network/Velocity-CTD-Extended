package com.velocitypowered.proxy.queue.cache;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.queue.MemoryQueue;
import com.velocitypowered.proxy.queue.Queue;
import com.velocitypowered.proxy.queue.exception.QueueCacheException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Elmar Blume - 03/04/2025
 */
public final class MemoryQueueCache implements QueueCache {

  private final VelocityServer server;
  private final Map<String, Queue> queues;

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
  public Queue getQueue(@NotNull String serverName) {
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


