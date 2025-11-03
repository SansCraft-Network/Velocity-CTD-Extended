package com.velocitypowered.proxy.xcd_queue;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.xcd_queue.model.QueuePlayer;
import com.velocitypowered.proxy.xcd_queue.model.QueueState;
import com.velocitypowered.proxy.xcd_queue.model.ServerStatus;
import com.velocitypowered.proxy.xcd_redis.impl.depot.PlayerEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.UUID;

/**
 * @author Elmar Blume - 10/10/2025
 */
public sealed interface Queue permits AbstractQueue {

  void enqueue(final UUID uniqueId);

  default void enqueue(final @NotNull Player player) {
    enqueue(player.getUniqueId());
  }

  default void enqueue(final @NotNull PlayerEntry playerEntry) {
    enqueue(playerEntry.getUniqueId());
  }

  void dequeue(final UUID uniqueId, boolean maxRetriesReached);

  default void dequeue(final Player player, boolean maxRetriesReached) {
    dequeue(player.getUniqueId(), maxRetriesReached);
  }

  default void dequeue(final PlayerEntry playerEntry, boolean maxRetriesReached) {
    dequeue(playerEntry.getUniqueId(), maxRetriesReached);
  }

  boolean contains(final UUID uniqueId);

  default boolean contains(final @NotNull Player player) {
    return contains(player.getUniqueId());
  }

  default boolean contains(final @NotNull PlayerEntry playerEntry) {
    return contains(playerEntry.getUniqueId());
  }

  void transferFirst(final QueuePlayer queuePlayer);

  QueuePlayer pollFirst();

  @NotNull
  @Unmodifiable
  Collection<QueuePlayer> getQueuePlayers();

  int getPosition(final UUID uniqueId);

  default int getPosition(final Player player) {
    return getPosition(player.getUniqueId());
  }

  VelocityRegisteredServer getBackendInstance();

  String getName();

  int size();

  boolean isOnline();

  boolean isPaused();

  boolean isFull();

  ServerStatus getStatus();

  void setStatus(final ServerStatus status);

  QueueState getState();

  void setState(final QueueState state);

  void stop();
}
