package com.velocitypowered.proxy.xcd_queue.depot;

import com.velocitypowered.proxy.xcd_queue.Queue;
import com.velocitypowered.proxy.xcd_queue.model.QueuePlayer;
import com.velocitypowered.proxy.xcd_redis.VelocityRedis;
import com.velocitypowered.proxy.xcd_redis.depot.AbstractDepotService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elmar Blume - 10/10/2025
 */
public final class QueueDepotService extends AbstractDepotService<String, QueueEntry> {

  public QueueDepotService(@NotNull VelocityRedis redis) {
    super(QueueEntry.class, redis.getProvider());
  }

  @Override
  public void teardown() {
    super.teardown();
  }

  public void insertQueueEntry(@NotNull Queue queue) {
    this.depot.upsert(new QueueEntry(queue));
  }

  public @Nullable QueueEntry getQueueEntry(@NotNull String queueName) {
    return this.depot.get(queueName);
  }

  public void upsertQueuePlayer(@NotNull QueuePlayer queuePlayer) {
    final QueueEntry queueEntry = this.getQueueEntry(queuePlayer.getQueueName());
    if (queueEntry == null) {
      return;
    }

    final QueuePlayer toUpdate = queueEntry.getQueuePlayer(queuePlayer.getUniqueId());
    if (toUpdate == null) {
      return;
    }

    toUpdate.copyFrom(queuePlayer);
    this.depot.upsert(queueEntry);
  }
}
