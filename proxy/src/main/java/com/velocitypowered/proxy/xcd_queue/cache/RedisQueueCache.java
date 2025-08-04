package com.velocitypowered.proxy.xcd_queue.cache;

import com.velocitypowered.proxy.xcd_queue.model.Queue;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * @author Elmar Blume - 03/04/2025
 */
public final class RedisQueueCache implements QueueCache {
  @Override
  public Queue getQueue(@NotNull String serverName) {
    return null;
  }

  @Override
  public Queue getQueue(@NotNull UUID playerUniqueId) {
    return null;
  }

  @Override
  public Collection<Queue> getQueues() {
    return List.of();
  }
}
