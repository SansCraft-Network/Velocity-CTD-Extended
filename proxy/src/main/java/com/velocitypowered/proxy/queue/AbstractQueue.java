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

package com.velocitypowered.proxy.queue;

import static com.velocitypowered.proxy.queue.model.QueueState.FULL;
import static com.velocitypowered.proxy.queue.model.QueueState.PAUSED;
import static com.velocitypowered.proxy.queue.model.ServerStatus.ONLINE;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.queue.manager.QueueManager;
import com.velocitypowered.proxy.queue.model.QueuePlayer;
import com.velocitypowered.proxy.queue.model.QueuePlayerData;
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
 */
public abstract sealed class AbstractQueue implements Queue
    permits MemoryQueue, RedisQueue {

  /**
   * The proxy server instance associated with this queue.
   */
  protected final VelocityServer server;

  /**
   * The queue manager responsible for coordinating queues and cache updates.
   */
  private final QueueManager<?> queueManager;

  /**
   * The backend server instance that this queue targets.
   */
  private final VelocityRegisteredServer backendInstance;

  /**
   * The underlying deque storing players currently in this queue.
   */
  private final Deque<QueuePlayer> internalQueue;

  /**
   * The current online/offline status of the backend server.
   */
  private ServerStatus serverStatus;

  /**
   * The current operational state of the queue (e.g., active, paused, full).
   */
  private QueueState state;

  /**
   * Lazily-initialized cached name of the backend server used for this queue.
   */
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

    this.serverStatus = ServerStatus.OFFLINE;
    this.state = server.getConfiguration().getQueue().getNoQueueServers()
        .contains(this.backendInstance.getServerInfo().getName()) ? QueueState.INACTIVE : QueueState.ACTIVE;
  }

  /**
   * Constructs a new {@link java.util.AbstractQueue} from a persisted {@link QueueEntry}.
   *
   * @param server          the proxy server instance
   * @param backendInstance the backend instance server
   * @param queueEntry      the persisted queue entry to initialize this queue from
   */
  public AbstractQueue(final VelocityServer server, final VelocityRegisteredServer backendInstance,
                       final @NotNull QueueEntry queueEntry) {
    this(server, backendInstance);

    this.serverStatus = queueEntry.getStatus();
    this.state = queueEntry.getState();

    this.internalQueue.clear();
    this.internalQueue.addAll(queueEntry.getDeque());
    this.internalQueue.forEach(queuePlayer -> queuePlayer.setContext(server, this));
  }

  /**
   * Enqueues a new player into this queue using the supplied {@link QueuePlayerData}.
   *
   * <p>The player is inserted based on their priority so that higher-priority players
   * are placed earlier in the queue. The queue cache is updated asynchronously after
   * the enqueue operation completes.</p>
   *
   * @param data the player data to enqueue
   */
  @Override
  public final void enqueue(final QueuePlayerData data) {
    final QueuePlayer queuePlayer = new QueuePlayer(this.server, this, data);

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

      if (!inserted) {
        internalQueue.addLast(queuePlayer);
      }

      CompletableFuture.runAsync(() -> queueManager.getQueueCache().updateQueue(this));
    }
  }

  /**
   * Inserts a {@link QueuePlayer} at the specified position in the queue.
   *
   * @param queuePlayer the queue player to insert
   * @param position    the position to insert the queue player at
   */
  private void insertAt(final QueuePlayer queuePlayer, final int position) {
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
      List<QueuePlayer> tempList = new ArrayList<>(internalQueue);
      tempList.add(position, queuePlayer);
      internalQueue.clear();
      internalQueue.addAll(tempList);
    }
  }

  /**
   * Dequeues a player from this queue by their unique identifier and optionally
   * notifies them if they have reached the maximum number of connection retries.
   *
   * @param uniqueId          the unique identifier of the player to remove
   * @param maxRetriesReached {@code true} if the player reached the maximum retries
   */
  @Override
  public void dequeue(final UUID uniqueId, final boolean maxRetriesReached) {
    if (maxRetriesReached) {
      server.getScheduler().buildTask(VelocityVirtualPlugin.INSTANCE, () -> notifyMaxRetriesReached(uniqueId))
          .delay(1, TimeUnit.SECONDS).schedule();
    }

    internalQueue.removeIf(queuePlayer -> queuePlayer.getUniqueId().equals(uniqueId));

    CompletableFuture.runAsync(() -> queueManager.getQueueCache().updateQueue(this));
  }

  /**
   * Checks whether this queue currently contains a player with the given unique identifier.
   *
   * @param uniqueId the unique identifier of the player
   * @return {@code true} if the player is in the queue, otherwise {@code false}
   */
  @Override
  public boolean contains(final UUID uniqueId) {
    for (QueuePlayer queuePlayer : internalQueue) {
      if (queuePlayer.getUniqueId().equals(uniqueId)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Attempts to transfer the given {@link QueuePlayer} to the backend server,
   * if they are not already waiting for a connection.
   *
   * @param queuePlayer the player to transfer
   */
  @Override
  public void transferFirst(final QueuePlayer queuePlayer) {
    if (queuePlayer.isWaitingForConnection()) {
      return;
    }

    queuePlayer.transfer();
  }

  /**
   * Retrieves and removes the first player in the queue, or returns {@code null}
   * if the queue is empty.
   *
   * @return the first {@link QueuePlayer}, or {@code null} if none
   */
  @Override
  public QueuePlayer pollFirst() {
    return this.internalQueue.pollFirst();
  }

  /**
   * Retrieves the {@link QueuePlayer} associated with the given unique identifier.
   *
   * @param uniqueId the unique identifier of the player
   * @return the queue player, or {@code null} if not found
   */
  @Override
  public @Nullable QueuePlayer getQueuePlayer(final UUID uniqueId) {
    for (QueuePlayer queuePlayer : internalQueue) {
      if (queuePlayer.getUniqueId().equals(uniqueId)) {
        return queuePlayer;
      }
    }

    return null;
  }

  /**
   * Returns an unmodifiable snapshot of all players currently in this queue.
   *
   * @return an unmodifiable collection of queue players
   */
  @Override
  public @NotNull @Unmodifiable Collection<QueuePlayer> getQueuePlayers() {
    return List.copyOf(this.internalQueue.stream().toList());
  }

  /**
   * Gets the one-based position of the player with the given unique identifier in this queue.
   *
   * @param uniqueId the unique identifier of the player
   * @return the position in the queue starting at 1, or {@code -1} if not found
   */
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

  /**
   * Returns the backend server instance associated with this queue.
   *
   * @return the backend {@link VelocityRegisteredServer}
   */
  @Override
  public VelocityRegisteredServer getBackendInstance() {
    return backendInstance;
  }

  /**
   * Gets the name of this queue, which corresponds to the backend server name.
   *
   * @return the queue name
   */
  @Override
  public String getName() {
    if (this.name == null) {
      this.name = backendInstance.getServerInfo().getName();
    }

    return this.name;
  }

  /**
   * Returns the current number of players in this queue.
   *
   * @return the size of the queue
   */
  @Override
  public int size() {
    return this.internalQueue.size();
  }

  /**
   * Gets the current {@link ServerStatus} of the backend server represented by this queue.
   *
   * @return the current server status
   */
  @Override
  public ServerStatus getServerStatus() {
    return serverStatus;
  }

  /**
   * Updates the {@link ServerStatus} of the backend server and triggers a queue cache update
   * if the status has changed.
   *
   * @param status the new server status
   */
  @Override
  public void setServerStatus(final ServerStatus status) {
    if (this.serverStatus != status) {
      this.serverStatus = status;
      this.queueManager.getQueueCache().updateQueue(this);
    }
  }

  /**
   * Gets the current {@link QueueState} for this queue.
   *
   * @return the queue state
   */
  @Override
  public QueueState getState() {
    return state;
  }

  /**
   * Updates the {@link QueueState} of this queue and triggers a queue cache update
   * if the state has changed.
   *
   * @param state the new queue state
   */
  @Override
  public void setState(final QueueState state) {
    if (this.state != state) {
      this.state = state;
      this.queueManager.getQueueCache().updateQueue(this);
    }
  }

  /**
   * Clears all players from this queue and asynchronously updates the queue cache.
   */
  @Override
  public void teardown() {
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
  public Component calculateEta(final long position) {
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
  public Component createActionbarComponent(final @NotNull QueuePlayer queuePlayer) {
    int position = getPosition(queuePlayer.getUniqueId());
    if (queuePlayer.isQueueBypass()) {
      return Component.translatable("velocity.queue.player-status.bypass", NamedTextColor.YELLOW);
    } else if (state == FULL && !queuePlayer.isFullBypass()) {
      return Component.translatable("velocity.queue.player-status.full", NamedTextColor.YELLOW)
          .arguments(
              Component.text(position),
              Component.text(this.size()),
              Component.text(this.getName()),
              calculateEta(position)
          );
    } else if (queuePlayer.isWaitingForConnection()) {
      return Component.translatable("velocity.queue.player-status.connecting", NamedTextColor.YELLOW)
          .arguments(Component.text(this.getName()));
    } else if (state == PAUSED) {
      return Component.translatable("velocity.queue.player-status.paused", NamedTextColor.YELLOW);
    } else if (serverStatus == ONLINE) {
      return Component.translatable("velocity.queue.player-status.online", NamedTextColor.YELLOW)
          .arguments(
              Component.text(position),
              Component.text(this.size()),
              Component.text(this.getName()),
              calculateEta(position)
          );
    } else {
      return Component.translatable("velocity.queue.player-status.offline", NamedTextColor.YELLOW)
          .arguments(
              Component.text(position),
              Component.text(this.size()),
              Component.text(this.getName())
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
                                Argument.string("paused", state == PAUSED ? "True" : "False"),
                                Argument.string("online", serverStatus == ONLINE ? "True" : "False")
                            ).asHoverEvent()
                    )
            )
        );
  }

  /**
   * Notifies the player that they have reached the maximum number of connection retries.
   *
   * @param uniqueId the unique id of the player to notify
   * @see MemoryQueue#notifyMaxRetriesReached(UUID)
   * @see RedisQueue#notifyMaxRetriesReached(UUID)
   */
  protected abstract void notifyMaxRetriesReached(final UUID uniqueId);
}
