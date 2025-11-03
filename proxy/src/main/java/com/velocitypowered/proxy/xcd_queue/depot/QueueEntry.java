package com.velocitypowered.proxy.xcd_queue.depot;

import com.velocitypowered.proxy.xcd_queue.Queue;
import com.velocitypowered.proxy.xcd_queue.model.QueuePlayer;
import com.velocitypowered.proxy.xcd_queue.model.QueueState;
import com.velocitypowered.proxy.xcd_queue.model.ServerStatus;
import com.velocitypowered.proxy.xcd_redis.depot.DepotEntry;
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
