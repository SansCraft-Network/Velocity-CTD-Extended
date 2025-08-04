package com.velocitypowered.proxy.xcd_queue.cache;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.xcd_queue.exception.QueueCacheException;
import com.velocitypowered.proxy.xcd_queue.model.Queue;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Elmar Blume - 03/04/2025
 */
public final class MemoryQueueCache implements QueueCache {

  private final VelocityServer proxy;
  private final Map<String, Queue> queues;

  /**
   * Constructs a new {@link MemoryQueueCache}
   *
   * @param proxy the proxy instance
   */
  public MemoryQueueCache(VelocityServer proxy) {
    this.proxy = proxy;
    this.queues = new HashMap<>();
  }

  @Override
  public Queue getQueue(@NotNull String serverName) {
//    final VelocityRegisteredServer server = (VelocityRegisteredServer) this.proxy.getServer(serverName)
//            .orElseThrow(() -> new QueueCacheException(serverName));
    return this.queues.get(serverName);
  }

  @Override
  public Queue getQueue(@NotNull UUID playerUniqueId) {
    return null;//todo
  }

  @Override
  public Collection<Queue> getQueues() {
    return List.copyOf(this.queues.values());
  }
}


