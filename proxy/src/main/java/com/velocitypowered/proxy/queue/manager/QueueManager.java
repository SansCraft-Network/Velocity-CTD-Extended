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

package com.velocitypowered.proxy.queue.manager;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.queue.Queue;
import com.velocitypowered.proxy.queue.cache.QueueCache;
import com.velocitypowered.proxy.queue.model.QueuePlayer;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import java.util.function.Function;
import net.kyori.adventure.text.Component;

/**
 * Represents the manager of the {@link Queue}-system.
 *
 * @author Elmar Blume - 02/04/2025
 * @see AbstractQueueManager
 * @see MemoryQueueManager
 * @see RedisQueueManager
 */
public sealed interface QueueManager<C extends QueueCache> permits AbstractQueueManager {

  /**
   * Reloads the queue manager.
   */
  void reload();

  /**
   * Tears down the queue manager, releasing any held resources.
   */
  void teardown();

  /**
   * Checks whether the current proxy is a master proxy.
   *
   * @return {@code true} if the current proxy is a master proxy, otherwise {@code false}
   */
  boolean isMasterProxy();

  /**
   * Removes and processes the first {@link QueuePlayer} in the specified {@link Queue}.
   * This method handles dequeueing the first player from the queue and initiates any
   * necessary procedures associated with their removal or transfer.
   *
   * @param queue the queue from which the first player will be polled
   * @param queuePlayer the queue player instance to handle during this operation
   */
  void pollFirst(final Queue queue, final QueuePlayer queuePlayer);

  /**
   * Adds a player to the queue for the specified server.
   *
   * @param player the player to be added to the queue
   * @param server the server for which the player is being queued
   */
  void queue(final Player player, final VelocityRegisteredServer server);

  /**
   * Gets the {@link QueueCache} instance.
   *
   * @return the {@link QueueCache} instance
   */
  C getQueueCache();

  /**
   * Handles the disconnection event of a player. This method is called when a player
   * disconnects from the system, allowing for cleanup or other necessary operations
   * specific to the disconnected player.
   *
   * @param player the player who has disconnected
   */
  void onPlayerDisconnect(final Player player);

  /**
   * Completely removes a player from the queue system and all associated data or references.
   * This method ensures that the specified player is entirely purged from all
   * managed queues or states within the system.
   *
   * @param player the player to be removed entirely from the queue system
   */
  void removePlayerEntirely(final Player player);

  /**
   * Broadcasts a message to all players in the specified queue. The message is generated
   * for each player using the provided {@link Function}.
   *
   * @param queue the queue whose players will receive the broadcasted message
   * @param component a function that generates the message {@link Component} for each
   *                  {@link QueuePlayer} in the specified queue
   */
  void broadcastMessage(final Queue queue, final Function<QueuePlayer, Component> component);

  /**
   * Broadcasts an action bar message to all players in the specified queue.
   * The message is generated for each player using the provided {@link Function}.
   *
   * @param queue the queue whose players will receive the broadcasted message
   * @param component a function that generates the message {@link Component} for each
   */
  void broadcastActionBar(final Queue queue, final Function<QueuePlayer, Component> component);
}
