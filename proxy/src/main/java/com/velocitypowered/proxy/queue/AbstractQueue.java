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

package com.velocitypowered.proxy.queue;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.queue.manager.QueueManager;
import com.velocitypowered.proxy.queue.model.QueuePlayer;
import com.velocitypowered.proxy.queue.model.QueueState;
import com.velocitypowered.proxy.queue.model.ServerStatus;
import com.velocitypowered.proxy.queue.redis.depot.QueueEntry;
import com.velocitypowered.proxy.queue.util.QueueTimeFormatter;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents an abstract queue of a {@link VelocityRegisteredServer}.
 *
 * @author Elmar Blume - 03/04/2025
 */
public abstract sealed class AbstractQueue implements Queue
    permits MemoryQueue, RedisQueue {

  protected final VelocityServer server;
  private final QueueManager<?> queueManager;
  private final VelocityRegisteredServer backendInstance;
  private final Deque<QueuePlayer> internalQueue;

  private ServerStatus status;
  private QueueState state;

  private transient String name;

  /**
   * Constructs a new {@link java.util.AbstractQueue}.
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
    this.state = server.getConfiguration().getQueue().getNoQueueServers()
            .contains(this.backendInstance.getServerInfo().getName()) ? QueueState.INACTIVE : QueueState.ACTIVE;
  }

  /**
   * Constructs a new {@link java.util.AbstractQueue}.
   *
   * @param server          the proxy server instance
   * @param backendInstance the backend instance server
   */
  public AbstractQueue(final VelocityServer server, final VelocityRegisteredServer backendInstance,
                       final @NotNull QueueEntry queueEntry) {
    this(server, backendInstance);

    // Copy from the queue entry
    this.status = queueEntry.getStatus();
    this.state = queueEntry.getState();
    this.internalQueue.clear();
    this.internalQueue.addAll(queueEntry.getDeque());
  }

  @Override
  public final void enqueue(final UUID uniqueId) {
    final Player player = this.server.getPlayer(uniqueId).orElseThrow();
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

  /**
   * Inserts a {@link QueuePlayer} at the specified position in the queue.
   *
   * @param queuePlayer the queue player to insert
   * @param position the position to insert the queue player at
   */
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
  public void dequeue(final UUID uniqueId, boolean maxRetriesReached) {
    final Player player = this.server.getPlayer(uniqueId).orElseThrow();

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
  public boolean contains(UUID uniqueId) {
    for (QueuePlayer queuePlayer : internalQueue) {
      if (queuePlayer.getUniqueId().equals(uniqueId)) {
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
  public @Nullable QueuePlayer getQueuePlayer(UUID uniqueId) {
    for (QueuePlayer queuePlayer : internalQueue) {
      if (queuePlayer.getUniqueId().equals(uniqueId)) {
        return queuePlayer;
      }
    }

    return null;
  }

  @Override
  public @NotNull @Unmodifiable Collection<QueuePlayer> getQueuePlayers() {
    return List.copyOf(this.internalQueue.stream().toList());
  }

  @Override
  public int getPosition(final UUID uniqueId) {
    int position = 1;

    for (QueuePlayer queuePlayer : internalQueue) {
      if (queuePlayer.getUniqueId().equals(uniqueId)) {
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
  public QueueState getState() {
    return state;
  }

  @Override
  public void setState(QueueState state) {
    if (this.state != state) {
      this.state = state;
      this.queueManager.getQueueCache().updateQueue(this);
    }
  }

  @Override
  public void clear() {
    internalQueue.clear();
    CompletableFuture.runAsync(() -> queueManager.getQueueCache().updateQueue(this));
  }

  /**
   * Calculates the estimated time remaining (ETA) for a player at a specific position in the queue.
   * The ETA is based on the server queue configurations send-delay multiplied by the position,
   * with a minimum value of 0 seconds.
   *
   * @param position the position of the player in the queue
   * @return a {@link Component} representing the formatted ETA
   */
  @ApiStatus.Internal
  public Component calculateEta(long position) {
    long delayInSeconds = (long) server.getConfiguration().getQueue().getSendDelay() * position;
    return QueueTimeFormatter.format(Math.max(delayInSeconds, 0));
  }

  /**
   * Creates an action bar {@link Component} for the given {@link QueuePlayer} based on their
   * current queue status and position. The component contains relevant information such as
   * the player's position, queue size, server name, and estimated time remaining (ETA), depending
   * on the specific state of the player and the queue.
   *
   * @param queuePlayer the {@link QueuePlayer} for whom the action bar component will be created, must not be null
   * @return a {@link Component} representing the player's current queue status in the action bar
   */
  @ApiStatus.Internal
  public Component createActionbarComponent(@NotNull QueuePlayer queuePlayer) {
    int position = getPosition(queuePlayer.getUniqueId());
    if (queuePlayer.isQueueBypass()) {
      return Component.translatable("velocity.queue.player-status.bypass", NamedTextColor.YELLOW);
    } else if (this.isFull() && !queuePlayer.isFullBypass()) {
      return Component.translatable("velocity.queue.player-status.full", NamedTextColor.YELLOW)
              .arguments(
                      Argument.numeric("position", position),
                      Argument.numeric("size", this.size()),
                      Argument.string("server", this.getName()),
                      Argument.component("eta", calculateEta(position))
              );
    } else if (queuePlayer.isWaitingForConnection()) {
      return Component.translatable("velocity.queue.player-status.connecting", NamedTextColor.YELLOW)
              .arguments(Argument.string("server", this.getName()));
    } else if (isPaused()) {
      return Component.translatable("velocity.queue.player-status.paused", NamedTextColor.YELLOW);
    } else if (isOnline()) {
      return Component.translatable("velocity.queue.player-status.online", NamedTextColor.YELLOW)
              .arguments(
                      Argument.numeric("position", position),
                      Argument.numeric("size", this.size()),
                      Argument.string("server", this.getName()),
                      Argument.component("eta", calculateEta(position))
              );
    } else {
      return Component.translatable("velocity.queue.player-status.offline", NamedTextColor.YELLOW)
              .arguments(
                      Argument.numeric("position", position),
                      Argument.numeric("size", this.size()),
                      Argument.string("server", this.getName())
              );
    }
  }

  /**
   * Creates a {@link Component} representing an entry in the list of queues.
   * The component includes the server name and additional metadata such as
   * the queue size, pause state, and online state, which are displayed as
   * hoverable details.
   *
   * @return a {@link Component} representing a formatted list entry for queues
   */
  @ApiStatus.Internal
  public Component createListComponent() {
    return Component.translatable("velocity.queue.command.listqueues.item")
            .arguments(
                    Argument.component("server",
                            Component.text(backendInstance.getServerInfo().getName())
                                    .hoverEvent(
                                            Component.translatable("velocity.queue.command.listqueues.hover")
                                                    .arguments(
                                                            Argument.numeric("size", size()),
                                                            Argument.string("paused", isPaused() ? "True" : "False"),
                                                            Argument.string("online", isOnline() ? "True" : "False")
                                                    ).asHoverEvent()
                                    )
                    )
            );
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
}
