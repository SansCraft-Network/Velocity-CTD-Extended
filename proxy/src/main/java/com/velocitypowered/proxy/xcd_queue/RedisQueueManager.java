package com.velocitypowered.proxy.xcd_queue;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.xcd_queue.cache.RedisQueueCache;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Elmar Blume - 02/04/2025
 */
public final class RedisQueueManager extends AbstractQueueManager<RedisQueueCache> {

  public RedisQueueManager(@NotNull VelocityServer proxy) {
    super(proxy);
  }

  @Override
  public boolean isMasterProxy() {
    return false;
  }

  @Override
  public void tick() {

  }

  @Override
  public RedisQueueCache getQueueCache() {
    return null;
  }
}
