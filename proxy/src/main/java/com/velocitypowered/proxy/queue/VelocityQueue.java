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

import static com.velocitypowered.api.queue.QueueState.PAUSED;
import static com.velocitypowered.api.queue.ServerStatus.FULL;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.queue.Queue;
import com.velocitypowered.api.queue.QueueEntry;
import com.velocitypowered.api.queue.QueueEntryData;
import com.velocitypowered.api.queue.QueueState;
import com.velocitypowered.api.queue.ServerStatus;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.queue.redis.depot.VelocityQueueDepotEntry;
import com.velocitypowered.proxy.queue.redis.packet.VelocityQueueSync;
import com.velocitypowered.proxy.queue.util.QueueTimeFormatter;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Single implementation of {@link Queue}.
 *
 * <p>Maintains an in-memory ordered list of {@link VelocityQueueEntry} objects. In Redis mode,
 * every mutation is also broadcast to other proxies via a
 * {@link VelocityQueueSync} pub/sub packet so that all proxies have an identical
 * local copy. The pub/sub approach avoids the read-modify-write race condition that
 * exists when two proxies try to overwrite the same Redis hash field simultaneously.</p>
 */
public final class VelocityQueue implements Queue {

  private final VelocityServer server;
  private final VelocityQueueManager manager;
  private final VelocityRegisteredServer backend;

  /**
   * The primary in-memory player list. Always accessed under {@code synchronized(players)}.
   */
  private final ConcurrentLinkedDeque<VelocityQueueEntry> players;

  private volatile ServerStatus serverStatus;
  private volatile QueueState state;

  /**
   * Creates a fresh queue for the given backend server.
   */
  public VelocityQueue(final VelocityServer server, final VelocityQueueManager manager,
                       final VelocityRegisteredServer backend, final QueueState initialState) {
    this.server = server;
    this.manager = manager;
    this.backend = backend;
    this.players = new ConcurrentLinkedDeque<>();
    this.serverStatus = ServerStatus.OFFLINE;
    this.state = initialState;
  }

  /**
   * Reconstructs a queue from a persisted {@link VelocityQueueDepotEntry} (Redis cold start).
   */
  public VelocityQueue(final VelocityServer server, final VelocityQueueManager manager,
                       final VelocityRegisteredServer backend,
                       final VelocityQueueDepotEntry entry) {
    this(server, manager, backend, entry.getState());
    this.serverStatus = entry.getServerStatus();
    for (VelocityQueueEntry queueEntry : entry.getEntries()) {
      queueEntry.setContext(server, this);
      this.players.addLast(queueEntry);
    }
  }

  @Override
  public String getName() {
    return backend.getServerInfo().getName();
  }

  @Override
  public RegisteredServer getServer() {
    return backend;
  }

  @Override
  public void enqueue(final @NotNull Player player) {
    enqueue(createQueueEntryData(player));
  }

  @Override
  public void enqueue(final @NotNull QueueEntryData data) {
    final VelocityQueueEntry entry = new VelocityQueueEntry(server, this, data);
    insertByPriority(entry);

    if (server.isRedisEnabled()) {
      new VelocityQueueSync(VelocityQueueSync.Payload.enqueue(
          getName(), data.uniqueId(), data.username(), data.priority(),
          data.fullBypass(), data.queueBypass()
      )).publish();
      persistAsync();
    }
  }

  @Override
  public void dequeue(final @NotNull UUID uniqueId) {
    synchronized (players) {
      players.removeIf(p -> p.getUniqueId().equals(uniqueId));
    }

    if (server.isRedisEnabled()) {
      new VelocityQueueSync(VelocityQueueSync.Payload.dequeue(getName(), uniqueId)).publish();
      persistAsync();
    }
  }

  @Override
  public boolean contains(final @NotNull UUID uniqueId) {
    for (VelocityQueueEntry entry : players) {
      if (entry.getUniqueId().equals(uniqueId)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @Nullable QueueEntry getEntry(final @NotNull UUID uniqueId) {
    for (VelocityQueueEntry entry : players) {
      if (entry.getUniqueId().equals(uniqueId)) {
        return entry;
      }
    }
    return null;
  }

  @Override
  public @NotNull @Unmodifiable Collection<QueueEntry> getEntries() {
    return List.copyOf(players);
  }

  @Override
  public Optional<Integer> getPosition(final @NotNull UUID uniqueId) {
    int pos = 1;
    for (VelocityQueueEntry entry : players) {
      if (entry.getUniqueId().equals(uniqueId)) {
        return Optional.of(pos);
      }
      pos++;
    }
    return Optional.empty();
  }

  @Override
  public int size() {
    return players.size();
  }

  @Override
  public @NotNull ServerStatus getServerStatus() {
    return serverStatus;
  }

  @Override
  public void setServerStatus(final @NotNull ServerStatus status) {
    if (this.serverStatus == status) {
      return;
    }
    this.serverStatus = status;

    if (server.isRedisEnabled()) {
      new VelocityQueueSync(VelocityQueueSync.Payload.statusChange(getName(), status)).publish();
      persistAsync();
    }
  }

  @Override
  public @NotNull QueueState getState() {
    return state;
  }

  @Override
  public void setState(final @NotNull QueueState state) {
    if (this.state == state) {
      return;
    }
    this.state = state;

    if (server.isRedisEnabled()) {
      new VelocityQueueSync(VelocityQueueSync.Payload.stateChange(getName(), state)).publish();
      persistAsync();
    }
  }

  @Override
  public void teardown() {
    synchronized (players) {
      players.clear();
    }
    if (server.isRedisEnabled()) {
      persistAsync();
    }
  }

  /**
   * Applies an ENQUEUE sync from another proxy. Does not re-publish.
   */
  @ApiStatus.Internal
  void applyEnqueue(final VelocityQueueSync.Payload p) {
    final QueueEntryData data = new QueueEntryData(p.playerUuid(), p.username(),
        p.priority(), p.fullBypass(), p.queueBypass());
    final VelocityQueueEntry entry = new VelocityQueueEntry(server, this, data);
    insertByPriority(entry);
  }

  /**
   * Applies a DEQUEUE sync from another proxy. Does not re-publish.
   */
  @ApiStatus.Internal
  void applyDequeue(final UUID uuid) {
    synchronized (players) {
      players.removeIf(p -> p.getUniqueId().equals(uuid));
    }
  }

  /**
   * Applies a STATE_CHANGE sync from another proxy. Does not re-publish.
   */
  @ApiStatus.Internal
  void applyStateChange(final QueueState newState) {
    this.state = newState;
  }

  /**
   * Applies a STATUS_CHANGE sync from another proxy. Does not re-publish.
   */
  @ApiStatus.Internal
  void applyStatusChange(final ServerStatus newStatus) {
    this.serverStatus = newStatus;
  }

  /**
   * Applies a WAITING_CHANGE sync from another proxy. Does not re-publish.
   */
  @ApiStatus.Internal
  void applyWaitingChange(final VelocityQueueSync.Payload p) {
    for (VelocityQueueEntry entry : players) {
      if (entry.getUniqueId().equals(p.playerUuid())) {
        entry.applyWaitingChangeFromPacket(p.waitingForConnection(), p.connectionAttempts(),
            p.updatedPriority(), p.updatedFullBypass(), p.updatedQueueBypass());
        break;
      }
    }
  }

  /**
   * Initiates the transfer of the given entry to the backend server, unless they are
   * already waiting for a connection.
   */
  @ApiStatus.Internal
  void transferEntry(final VelocityQueueEntry entry) {
    if (entry.isWaitingForConnection()) {
      return;
    }
    entry.transfer();
  }

  /**
   * Removes the given entry from the queue without notifying them.
   * Used when the player is no longer online on any proxy.
   */
  @ApiStatus.Internal
  void removeEntry(final VelocityQueueEntry entry) {
    players.removeIf(p -> p.getUniqueId().equals(entry.getUniqueId()));
    if (server.isRedisEnabled()) {
      new VelocityQueueSync(VelocityQueueSync.Payload.dequeue(getName(), entry.getUniqueId())).publish();
      persistAsync();
    }
  }

  /**
   * Computes the estimated time to transfer for the given position.
   */
  public Component calculateEta(final long position) {
    long delayInSeconds = (long) server.getConfiguration().getQueue().getSendDelay() * position;
    return QueueTimeFormatter.format(Math.max(delayInSeconds, 0));
  }

  /**
   * Creates the action bar component shown to the player at their current position.
   */
  public Component createActionbarComponent(final @NotNull VelocityQueueEntry entry) {
    final int position = getPosition(entry.getUniqueId()).orElseThrow();

    if (entry.isQueueBypass()) {
      return Component.translatable("velocity.queue.player-status.bypass", NamedTextColor.YELLOW);
    } else if (serverStatus == FULL && !entry.isFullBypass()) {
      return Component.translatable("velocity.queue.player-status.full", NamedTextColor.YELLOW)
          .arguments(
              Component.text(position),
              Component.text(this.size()),
              Component.text(this.getName()),
              calculateEta(position));
    } else if (entry.isWaitingForConnection()) {
      return Component.translatable("velocity.queue.player-status.connecting", NamedTextColor.YELLOW)
          .arguments(Component.text(this.getName()));
    } else if (state == PAUSED) {
      return Component.translatable("velocity.queue.player-status.paused", NamedTextColor.YELLOW);
    } else if (serverStatus.isActive()) {
      return Component.translatable("velocity.queue.player-status.online", NamedTextColor.YELLOW)
          .arguments(
              Component.text(position),
              Component.text(this.size()),
              Component.text(this.getName()),
              calculateEta(position));
    } else {
      return Component.translatable("velocity.queue.player-status.offline", NamedTextColor.YELLOW)
          .arguments(
              Component.text(position),
              Component.text(this.size()),
              Component.text(this.getName()));
    }
  }

  /**
   * Returns the raw internal list. Used by {@link VelocityQueueDepotEntry} for snapshotting.
   */
  @ApiStatus.Internal
  public List<VelocityQueueEntry> getInternalEntries() {
    return new ArrayList<>(players);
  }

  /**
   * Inserts the entry at the correct position according to descending priority order,
   * preserving insertion order (FIFO) within the same priority tier.
   */
  private void insertByPriority(final VelocityQueueEntry entry) {
    synchronized (players) {
      if (contains(entry.getUniqueId())) {
        return; // duplicate
      }

      final Iterator<VelocityQueueEntry> it = players.iterator();
      int position = 0;
      boolean inserted = false;

      while (it.hasNext()) {
        final VelocityQueueEntry current = it.next();
        if (current.getPriority() < entry.getPriority()) {
          insertAt(entry, position);
          inserted = true;
          break;
        }
        position++;
      }

      if (!inserted) {
        players.addLast(entry);
      }
    }
  }

  private void insertAt(final VelocityQueueEntry entry, final int position) {
    final List<VelocityQueueEntry> tempList = new ArrayList<>(players);
    tempList.add(position, entry);
    players.clear();
    players.addAll(tempList);
  }

  /**
   * Persists this queue's state to Redis asynchronously. Only the master proxy needs
   * to do this, but all proxies can safely call it (non-master calls are harmless overhead).
   */
  private void persistAsync() {
    if (manager.isMasterProxy()) {
      CompletableFuture.runAsync(() ->
          server.getRedis().getQueueService().upsertQueue(this));
    }
  }

  /**
   * Creates a {@link QueueEntryData} for the given player joining this queue.
   *
   * @param player the online player
   * @return a populated {@link QueueEntryData}
   */
  private @NotNull QueueEntryData createQueueEntryData(final @NotNull Player player) {
    return new QueueEntryData(
        player.getUniqueId(),
        player.getUsername(),
        player.getQueuePriority(getName()),
        player.hasPermission("velocity.queue.full.bypass"),
        player.hasPermission("velocity.queue.bypass")
    );
  }
}
