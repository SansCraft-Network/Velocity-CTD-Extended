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
import com.velocitypowered.proxy.queue.model.QueueState;
import com.velocitypowered.proxy.queue.model.ServerStatus;
import com.velocitypowered.proxy.redis.depot.DepotEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author Elmar Blume - 10/10/2025
 */
public final class QueueEntry extends DepotEntry<String, QueueEntry> {

  private final Deque<QueuePlayer> deque = new ConcurrentLinkedDeque<>();

  private final ServerStatus status;
  private final QueueState state;

  public QueueEntry(final @NotNull Queue queue) {
    super(queue.getName());
    this.deque.addAll(queue.getQueuePlayers());
    this.status = queue.getStatus();
    this.state = queue.getState();
  }

  public @Nullable QueuePlayer getQueuePlayer(final @NotNull UUID uniqueId) {
    for (QueuePlayer queuePlayer : deque) {
      if (queuePlayer.getUniqueId().equals(uniqueId)) {
        return queuePlayer;
      }
    }
    return null;
  }

  public Deque<QueuePlayer> getDeque() {
    return deque;
  }

  public ServerStatus getStatus() {
    return status;
  }

  public QueueState getState() {
    return state;
  }
}
