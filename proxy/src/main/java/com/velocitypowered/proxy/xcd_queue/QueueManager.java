package com.velocitypowered.proxy.xcd_queue;

import com.velocitypowered.proxy.xcd_queue.cache.QueueCache;

/**
 * Represents a manager for CTD's queuing system
 *
 * @author Elmar Blume - 02/04/2025
 * @see AbstractQueueManager
 * @see MemoryQueueManager
 * @see RedisQueueManager
 */
public sealed interface QueueManager<C extends QueueCache> permits AbstractQueueManager {

  boolean isMasterProxy();

  void tick();

  C getQueueCache();

}
