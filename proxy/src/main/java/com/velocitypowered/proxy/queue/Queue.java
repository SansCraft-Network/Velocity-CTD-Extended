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
import com.velocitypowered.proxy.queue.model.QueuePlayer;
import com.velocitypowered.proxy.queue.model.QueuePlayerData;
import com.velocitypowered.proxy.queue.model.QueueState;
import com.velocitypowered.proxy.queue.model.ServerStatus;
import com.velocitypowered.proxy.redis.impl.depot.PlayerEntry;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents a queue of a {@link VelocityRegisteredServer}.
 *
 * @author Elmar Blume - 10/10/2025
 */
public sealed interface Queue permits AbstractQueue {

  /**
   * Adds a player to the queue using their unique identifier.
   *
   * @param data the data of the player to be added to the queue
   */
  void enqueue(final QueuePlayerData data);

  /**
   * Adds a player to the queue using their player object.
   *
   * @param player the player to be added to the queue
   */
  default void enqueue(final @NotNull Player player) {
    enqueue(QueuePlayerData.of(this, player));
  }

  /**
   * Adds a player to the queue using their player entry object.
   *
   * @param playerEntry the player entry to be added to the queue
   */
  default void enqueue(final @NotNull PlayerEntry playerEntry) {
    enqueue(QueuePlayerData.of(this, playerEntry));
  }

  /**
   * Removes a player from the queue.
   *
   * @param uniqueId the unique identifier of the player to be removed from the queue
   * @param maxRetriesReached indicates whether the maximum number of retry attempts has been reached
   */
  void dequeue(final UUID uniqueId, boolean maxRetriesReached);

  /**
   * Removes a player from the queue.
   *
   * @param player the player to be removed from the queue
   * @param maxRetriesReached indicates whether the maximum number of retry attempts has been reached
   */
  default void dequeue(final Player player, boolean maxRetriesReached) {
    dequeue(player.getUniqueId(), maxRetriesReached);
  }

  /**
   * Removes a player from the queue.
   *
   * @param playerEntry the player entry to be removed from the queue
   * @param maxRetriesReached indicates whether the maximum number of retry attempts has been reached
   */
  default void dequeue(final PlayerEntry playerEntry, boolean maxRetriesReached) {
    dequeue(playerEntry.getUniqueId(), maxRetriesReached);
  }

  /**
   * Checks if the queue contains a player identified by the specified unique identifier.
   *
   * @param uniqueId the unique identifier of the player to check
   * @return true if the player is present in the queue; false otherwise
   */
  boolean contains(final UUID uniqueId);

  /**
   * Checks if the queue contains a player identified by the specified player object.
   *
   * @param player the player object to check
   * @return true if the player is present in the queue; false otherwise
   */
  default boolean contains(final @NotNull Player player) {
    return contains(player.getUniqueId());
  }

  /**
   * Checks if the queue contains a player identified by the specified player entry object.
   *
   * @param playerEntry the player entry object to check
   * @return true if the player is present in the queue; false otherwise
   */
  default boolean contains(final @NotNull PlayerEntry playerEntry) {
    return contains(playerEntry.getUniqueId());
  }

  /**
   * Transfers the first player in the queue to the specified queue player.
   *
   * @param queuePlayer the queue player to transfer to
   */
  void transferFirst(final QueuePlayer queuePlayer);

  /**
   * Retrieves and removes the first player in the queue.
   *
   * @return the first player in the queue, or {@code null} if the queue is empty
   */
  QueuePlayer pollFirst();

  /**
   * Retrieves the first player in the queue.
   *
   * @param uniqueId the unique identifier of the player to retrieve
   * @return the first player in the queue, or {@code null} if the queue does not contain the player
   */
  @Nullable
  QueuePlayer getQueuePlayer(UUID uniqueId);

  /**
   * Retrieves all players in the queue as a immutable collection.
   *
   * @return a collection of all players in the queue
   */
  @NotNull
  @Unmodifiable
  Collection<QueuePlayer> getQueuePlayers();

  /**
   * Retrieves the position of a player in the queue based on their unique identifier.
   *
   * @param uniqueId the unique identifier of the player whose position in the queue is to be retrieved
   * @return the position of the player in the queue, or -1 if the player is not present in the queue
   */
  int getPosition(final UUID uniqueId);

  /**
   * Retrieves the position of a player in the queue based on their player object.
   *
   * @param player the player object whose position in the queue is to be retrieved
   * @return the position of the player in the queue, or -1 if the player is not present in the queue
   */
  default int getPosition(final Player player) {
    return getPosition(player.getUniqueId());
  }

  /**
   * Retrieves the backend instance of the {@link VelocityRegisteredServer} associated with the queue.
   *
   * @return the backend instance of the server
   */
  VelocityRegisteredServer getBackendInstance();

  /**
   * Retrieves the name of the {@link VelocityRegisteredServer} associated with the queue.
   *
   * @return the name of the server
   */
  String getName();

  /**
   * Retrieves the number of players in the queue.
   *
   * @return the number of players in the queue
   */
  int size();

  /**
   * Checks if the server or queue is currently online.
   *
   * @return true if the server or queue is online; false otherwise
   */
  boolean isOnline();

  /**
   * Checks if the server or queue is currently paused.
   *
   * @return true if the server or queue is paused; false otherwise
   */
  boolean isPaused();

  /**
   * Checks if the server or queue is currently full.
   *
   * @return true if the server or queue is full; false otherwise
   */
  boolean isFull();

  /**
   * Retrieves the current status of the backend server in relation to queuing.
   *
   * @return the current {@code ServerStatus} of the backend server, such as {@code OFFLINE}, {@code WAITING}, or {@code ONLINE}
   */
  ServerStatus getStatus();

  /**
   * Sets the current status of the backend server in relation to queuing.
   *
   * @param status the new {@code ServerStatus} of the backend server
   */
  void setStatus(final ServerStatus status);

  /**
   * Retrieves the current state of the queue.
   *
   * @return the current {@code QueueState} of the queue
   */
  QueueState getState();

  /**
   * Sets the current state of the queue.
   *
   * @param state the new {@code QueueState} of the queue
   */
  void setState(final QueueState state);

  /**
   * Tear down the queue.
   */
  void teardown();
}
