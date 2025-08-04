package com.velocitypowered.proxy.xcd_queue.model;

import com.velocitypowered.proxy.queue.ServerStatus;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Represents a queue of a {@link VelocityRegisteredServer}
 *
 * @author Elmar Blume - 03/04/2025
 */
public final class Queue {

  private final VelocityRegisteredServer server;
  private final Deque<QueueEntry> internalQueue;

  private ServerStatus status;
  private QueueState state;

  private transient String serverName;

  /**
   * Constructs a new {@link Queue}
   *
   * @param server the backend server
   */
  public Queue(VelocityRegisteredServer server) {
    this.server = server;
    this.internalQueue = new ConcurrentLinkedDeque<>();

    // Initialize the queue status and state
    this.status = ServerStatus.OFFLINE;
    this.state = QueueState.INACTIVE;
  }

  public String getServerName() {
    if (this.serverName == null) {
      this.serverName = server.getServerInfo().getName();
    }

    return this.serverName;
  }

  public int getSize() {
    return this.internalQueue.size();
  }

  public QueueState getState() {
    return state;
  }

  public ServerStatus getStatus() {
    return status;
  }

  public Deque<QueueEntry> getQueue() {
    return internalQueue;
  }

  public @NotNull @Unmodifiable Collection<QueueEntry> getEntries() {
    return List.copyOf(this.internalQueue.stream().toList());
  }
}
