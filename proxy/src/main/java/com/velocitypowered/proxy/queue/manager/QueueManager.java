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
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.queue.Queue;
import com.velocitypowered.proxy.queue.cache.QueueCache;
import com.velocitypowered.proxy.queue.model.QueuePlayer;
import net.kyori.adventure.text.Component;

import java.util.function.Function;

/**
 * Represents a manager for CTD's queuing system
 *
 * @author Elmar Blume - 02/04/2025
 * @see AbstractQueueManager
 * @see MemoryQueueManager
 * @see RedisQueueManager
 */
public sealed interface QueueManager<C extends QueueCache> permits AbstractQueueManager {

  void reload();

  boolean isMasterProxy();

  void pollFirst(final Queue queue, final QueuePlayer queuePlayer);

  void queue(final Player player, final VelocityRegisteredServer server);

  C getQueueCache();

  void onPlayerDisconnect(final Player player);

  void removePlayerEntirely(final Player player);

  void broadcastMessage(final Queue queue, final Function<QueuePlayer, Component> component);

  default void broadcastMessage(final Function<QueuePlayer, Component> component) {
    for (Queue queue : this.getQueueCache().getQueues()) {
      this.broadcastMessage(queue, component);
    }
  }

  void broadcastActionBar(final Queue queue, final Function<QueuePlayer, Component> component);

  default void broadcastActionBar(final Function<QueuePlayer, Component> component) {
    for (Queue queue : this.getQueueCache().getQueues()) {
      this.broadcastActionBar(queue, component);
    }
  }
}
