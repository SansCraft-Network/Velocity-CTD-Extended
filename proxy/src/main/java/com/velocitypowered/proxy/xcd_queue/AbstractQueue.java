package com.velocitypowered.proxy.xcd_queue;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.queue.QueueTimeFormatter;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.xcd_queue.manager.QueueManager;
import com.velocitypowered.proxy.xcd_queue.model.QueuePlayer;
import com.velocitypowered.proxy.xcd_queue.model.QueueState;
import com.velocitypowered.proxy.xcd_queue.model.ServerStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Represents a queue of a {@link VelocityRegisteredServer}
 *
 * @author Elmar Blume - 03/04/2025
 */
public sealed abstract class AbstractQueue implements Queue permits MemoryQueue, RedisQueue {

  private final VelocityServer server;
  private final QueueManager<?> queueManager;
  private final VelocityRegisteredServer backendInstance;
  private final Deque<QueuePlayer> internalQueue;

  private ServerStatus status;
  private QueueState state;

  private transient String name;

  /**
   * Constructs a new {@link java.util.AbstractQueue}
   *
   * @param server          the proxy server instance
   * @param backendInstance the backend instance server
   */
  public AbstractQueue(final VelocityServer server, final VelocityRegisteredServer backendInstance) {
    this.server = server;
    this.queueManager = server.getQueueManager();
    this.backendInstance = backendInstance;
    this.internalQueue = new ConcurrentLinkedDeque<>();

    // Initialize the queue status and state
    this.status = ServerStatus.OFFLINE;
    this.state = QueueState.INACTIVE;
  }

  @Override
  public final void enqueue(final Player player) {
    final QueuePlayer queuePlayer = new QueuePlayer(this.server, player, this);

    synchronized (internalQueue) {
      final Iterator<QueuePlayer> iterator = internalQueue.iterator();
      boolean inserted = false;
      int position = 0;

      if (iterator.hasNext()) {
        do {
          final QueuePlayer currentPlayer = iterator.next();
          if (currentPlayer.getPriority() < queuePlayer.getPriority()) {
            insertAt(queuePlayer, position);
            inserted = true;
            break;
          }

          position++;
        } while (iterator.hasNext());
      }

      // Append to the end if not inserted
      if (!inserted) {
        internalQueue.addLast(queuePlayer);
      }

      // Update cache asynchronously
      CompletableFuture.runAsync(() -> queueManager.getQueueCache().updateQueue(this));
    }
  }

  private void insertAt(final QueuePlayer queuePlayer, final int position) {
    // For small queues, use the existing approach
    if (internalQueue.size() < 100) {
      final ConcurrentLinkedQueue<QueuePlayer> tempQueue = new ConcurrentLinkedQueue<>();

      int index = 0;
      for (QueuePlayer existingQueuePlayer : internalQueue) {
        if (index == position) {
          tempQueue.add(queuePlayer);
        }
        tempQueue.add(existingQueuePlayer);
        index++;
      }

      internalQueue.clear();
      internalQueue.addAll(tempQueue);
    } else {
      // Fallback for large queues
      List<QueuePlayer> tempList = new ArrayList<>(internalQueue);
      tempList.add(position, queuePlayer);
      internalQueue.clear();
      internalQueue.addAll(tempList);
    }
  }

  @Override
  public void dequeue(final Player player, boolean maxRetriesReached) {
    // Notify the player if max retries have been reached
    if (maxRetriesReached) {
      server.getScheduler().buildTask(VelocityVirtualPlugin.INSTANCE, () -> notifyMaxRetriesReached(player))
              .delay(1, TimeUnit.SECONDS).schedule();
    }

    // Remove the player from the internal queue
    internalQueue.removeIf(queuePlayer -> queuePlayer.getUniqueId().equals(player.getUniqueId()));

    // Update cache asynchronously
    CompletableFuture.runAsync(() -> queueManager.getQueueCache().updateQueue(this));
  }

  @Override
  public boolean contains(Player player) {
    for (QueuePlayer queuePlayer : internalQueue) {
      if (queuePlayer.getUniqueId().equals(player.getUniqueId())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void transferFirst(QueuePlayer queuePlayer) {
    if (queuePlayer.isWaitingForConnection()) {
      return;
    }

    // Mark the player as waiting for connection
    queuePlayer.transfer();
  }

  @Override
  public QueuePlayer pollFirst() {
    return this.internalQueue.pollFirst();
  }

  @Override
  public @NotNull @Unmodifiable Collection<QueuePlayer> getQueuePlayers() {
    return List.copyOf(this.internalQueue.stream().toList());
  }

  @Override
  public int getPosition(Player player) {
    int position = 1;

    for (QueuePlayer queuePlayer : internalQueue) {
      if (queuePlayer.getUniqueId().equals(player.getUniqueId())) {
        return position;
      }
      position++;
    }

    return -1;
  }

  @Override
  public VelocityRegisteredServer getBackendInstance() {
    return backendInstance;
  }

  @Override
  public String getName() {
    if (this.name == null) {
      this.name = backendInstance.getServerInfo().getName();
    }

    return this.name;
  }

  @Override
  public int size() {
    return this.internalQueue.size();
  }

  @Override
  public boolean isOnline() {
    return status == ServerStatus.ONLINE;
  }

  @Override
  public boolean isPaused() {
    return state == QueueState.PAUSED;
  }

  @Override
  public boolean isFull() {
    return state == QueueState.FULL;
  }

  @Override
  public ServerStatus getStatus() {
    return status;
  }

  @Override
  public void setStatus(ServerStatus status) {
    if (this.status != status) {
      this.status = status;
      this.queueManager.getQueueCache().updateQueue(this);
    }
  }

  @Override
  public void stop() {
    internalQueue.clear();
    CompletableFuture.runAsync(() -> queueManager.getQueueCache().updateQueue(this));
  }

  @Override
  public Component calculateEta(long position) {
    long delayInSeconds = (long) server.getConfiguration().getQueue().getSendDelay() * position;
    return QueueTimeFormatter.format(Math.max(delayInSeconds, 0));
  }

  @Override
  public Component getActionBarComponent(QueuePlayer queuePlayer) {
    return null;//todo
  }

  /**
   * Notifies the player that they have reached the maximum number of connection retries.
   *
   * @param player the player to notify
   * @see MemoryQueue#notifyMaxRetriesReached(Player)
   * @see RedisQueue#notifyMaxRetriesReached(Player)
   */
  protected void notifyMaxRetriesReached(final Player player) {
    // empty implementation, should be overridden by subclasses - memory, redis
  }

  public QueueState getState() {
    return state;
  }

  public Deque<QueuePlayer> getQueue() {
    return internalQueue;
  }
}
