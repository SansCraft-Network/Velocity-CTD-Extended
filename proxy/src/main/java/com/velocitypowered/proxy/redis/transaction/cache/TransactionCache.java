/*
 * Copyright (C) 2025 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.redis.transaction.cache;

import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.redis.VelocityRedis;
import com.velocitypowered.proxy.redis.transaction.Transaction;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;

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
