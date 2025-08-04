package com.velocitypowered.proxy.xcd_redis.transaction.cache;

import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.xcd_redis.VelocityRedis;
import com.velocitypowered.proxy.xcd_redis.transaction.Transaction;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * @author Elmar Blume - 13/05/2025
 */
public final class TransactionCache extends HashMap<UUID, Transaction<?, ?>> {

  private final @MonotonicNonNull HashMap<UUID, ScheduledTask> refreshTasks;

  private final double delay;
  private final TimeUnit timeUnit;

  private BiConsumer<UUID, Transaction<?, ?>> purgeConsumer;

  public TransactionCache() {
    this(Transaction.DEFAULT_TIMEOUT, Transaction.DEFAULT_TIME_UNIT);
  }

  public TransactionCache(double delay, TimeUnit timeUnit) {
    this.refreshTasks = new HashMap<>();
    this.delay = delay;
    this.timeUnit = timeUnit;
  }

  public TransactionCache(BiConsumer<UUID, Transaction<?, ?>> purgeConsumer) {
    this();
    this.purgeConsumer = purgeConsumer;
  }

  @Override
  public Transaction<?, ?> put(UUID key, Transaction<?, ?> value) {
    this.queue(value, this.delay, this.timeUnit);
    return super.put(key, value);
  }

  public Transaction<?, ?> put(@NotNull Transaction<?, ?> value, int delay, TimeUnit timeUnit) {
    this.queue(value, delay, timeUnit);
    return super.put(value.getTransactionId(), value);
  }

  @Override
  public Transaction<?, ?> remove(Object key) {
    final ScheduledTask scheduledTask = this.refreshTasks.remove(key);

    // Cancel any refresh task
    if (scheduledTask != null) {
      scheduledTask.cancel();
    }

    return super.remove(key);
  }

  private void queue(@NotNull Transaction<?, ?> transaction, double delay, TimeUnit timeUnit) {
    final UUID key = transaction.getTransactionId();

    // Make sure to remove previous values
    if (this.refreshTasks.containsKey(key)) {
      this.refreshTasks.get(key).cancel();
      this.refreshTasks.remove(key);
    }

    // Schedule the task
    final ScheduledTask scheduledTask = VelocityRedis.INSTANCE.getServer().getScheduler()
            .buildTask(VelocityVirtualPlugin.INSTANCE, () -> {
              this.remove(key);
              this.purgeConsumer.accept(key, transaction);
            })
            .delay((long) delay, timeUnit)
            .schedule();

    this.refreshTasks.put(key, scheduledTask);
  }

}
