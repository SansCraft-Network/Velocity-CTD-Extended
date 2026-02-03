/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.queue.Queue;
import com.velocitypowered.proxy.queue.model.QueuePlayer;
import com.velocitypowered.proxy.redis.VelocityRedis;
import com.velocitypowered.proxy.redis.depot.AbstractDepotService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a depot service for managing queue entries.
 */
public final class QueueDepotService extends AbstractDepotService<String, QueueEntry> {

  /**
   * The owning {@link VelocityServer} instance associated with this depot service.
   */
  private final VelocityServer server;

  /**
   * Constructs a new {@link QueueDepotService}.
   *
   * @param redis the redis provider implementation instance
   */
  public QueueDepotService(@NotNull final VelocityRedis redis) {
    super(QueueEntry.class, redis.getProvider());

    this.server = redis.getServer();
  }

  @Override
  public void teardown() {
    super.teardown();
  }

  /**
   * Inserts a new queue entry into the depot.
   *
   * @param queue the queue to insert
   */
  public void insertQueueEntry(final @NotNull Queue queue) {
    this.depot.upsert(new QueueEntry(queue));
  }

  /**
   * Retrieves a queue entry from the depot.
   *
   * @param queueName the name of the queue to retrieve
   * @return the queue entry, or {@code null} if the queue does not exist
   */
  public @Nullable QueueEntry getQueueEntry(final @NotNull String queueName) {
    return this.depot.get(queueName);
  }

  /**
   * Updates a queue player in the depot.
   *
   * @param queuePlayer the queue player to update
   */
  public void upsertQueuePlayer(final @NotNull QueuePlayer queuePlayer) {
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
