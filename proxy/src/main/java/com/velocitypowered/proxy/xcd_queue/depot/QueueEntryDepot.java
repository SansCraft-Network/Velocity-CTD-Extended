package com.velocitypowered.proxy.xcd_queue.depot;

import com.velocitypowered.proxy.xcd_redis.VelocityRedis;
import com.velocitypowered.proxy.xcd_redis.depot.AbstractDepotService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elmar Blume - 10/10/2025
 */
public final class QueueEntryDepot extends AbstractDepotService<String, QueueEntry> {

  public QueueEntryDepot(@NotNull VelocityRedis redis) {
    super(QueueEntry.class, redis.getProvider());
  }

  public @Nullable QueueEntry getQueueEntry(@NotNull String queueName) {
    return this.depot.get(queueName);
  }
}
