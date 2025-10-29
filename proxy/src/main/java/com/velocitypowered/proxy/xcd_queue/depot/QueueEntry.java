package com.velocitypowered.proxy.xcd_queue.depot;

import com.velocitypowered.proxy.xcd_queue.Queue;
import com.velocitypowered.proxy.xcd_queue.model.QueuePlayer;
import com.velocitypowered.proxy.xcd_queue.model.ServerStatus;
import com.velocitypowered.proxy.xcd_redis.depot.DepotEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author Elmar Blume - 10/10/2025
 */
public final class QueueEntry extends DepotEntry<String, QueueEntry> {

  private final Deque<QueuePlayer> deque = new ConcurrentLinkedDeque<>();

  private final ServerStatus status;
  private final boolean full;
  private final boolean paused;

  public QueueEntry(final @NotNull Queue queue) {
    super(queue.getName());
    this.deque.addAll(queue.getQueuePlayers());
    this.status = queue.getStatus();
    this.full = queue.isFull();
    this.paused = queue.isPaused();
  }

  public Deque<QueuePlayer> getDeque() {
    return deque;
  }

  public ServerStatus getStatus() {
    return status;
  }

  public boolean isFull() {
    return full;
  }

  public boolean isPaused() {
    return paused;
  }
}
