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

package com.velocitypowered.proxy.queue.redis.depot;

import com.velocitypowered.proxy.queue.Queue;
import com.velocitypowered.proxy.queue.model.QueuePlayer;
import com.velocitypowered.proxy.redis.VelocityRedis;
import com.velocitypowered.proxy.redis.depot.AbstractDepotService;
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
