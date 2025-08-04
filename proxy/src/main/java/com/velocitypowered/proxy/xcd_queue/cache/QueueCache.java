package com.velocitypowered.proxy.xcd_queue.cache;

import com.velocitypowered.proxy.xcd_queue.model.Queue;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

/**
 * Represents a cache that stores the queue data
 *
 * @author Elmar Blume - 03/04/2025
 */
public sealed interface QueueCache permits MemoryQueueCache, RedisQueueCache {

  Queue getQueue(@NotNull String serverName);

  Queue getQueue(@NotNull UUID playerUniqueId);

  Collection<Queue> getQueues();
}
