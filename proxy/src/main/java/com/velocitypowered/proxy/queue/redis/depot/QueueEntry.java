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
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a queue entry in the depot.
 *
 * @author Elmar Blume - 10/10/2025
 */
public final class QueueEntry extends DepotEntry<String, QueueEntry> {

  private final Deque<QueuePlayer> deque = new ConcurrentLinkedDeque<>();

  private final ServerStatus status;
  private final QueueState state;

  /**
   * Constructs a new {@link QueueEntry}
   *
   * @param queue the queue instance
   */
  public QueueEntry(final @NotNull Queue queue) {
    super(queue.getName());

    this.deque.addAll(queue.getQueuePlayers());
    this.status = queue.getStatus();
    this.state = queue.getState();
  }

  /**
   * Retrieves a {@link QueuePlayer} from the queue entry.
   *
   * @param uniqueId the unique identifier of the player to retrieve
   * @return the {@link QueuePlayer}, or {@code null} if not found
   */
  public @Nullable QueuePlayer getQueuePlayer(final @NotNull UUID uniqueId) {
    for (QueuePlayer queuePlayer : deque) {
      if (queuePlayer.getUniqueId().equals(uniqueId)) {
        return queuePlayer;
      }
    }
    return null;
  }

  /**
   * Gets the queue players.
   *
   * @return the queue players
   */
  public Deque<QueuePlayer> getDeque() {
    return deque;
  }

  /**
   * Gets the server status.
   *
   * @return the server status
   */
  public ServerStatus getStatus() {
    return status;
  }

  /**
   * Gets the queue state.
   *
   * @return the queue state
   */
  public QueueState getState() {
    return state;
  }
}
