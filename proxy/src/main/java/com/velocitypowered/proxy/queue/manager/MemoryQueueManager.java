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

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.queue.Queue;
import com.velocitypowered.proxy.queue.cache.MemoryQueueCache;
import com.velocitypowered.proxy.queue.model.QueuePlayer;
import java.util.function.Function;
import net.kyori.adventure.text.Component;

/**
 * Represents the in-memory implementation of {@link QueueManager} which uses a {@link MemoryQueueCache}.
 *
 * @author Elmar Blume - 02/04/2025
 */
public final class MemoryQueueManager extends AbstractQueueManager<MemoryQueueCache> {

  private final MemoryQueueCache queueCache;

  /**
   * Constructs a new {@link MemoryQueueManager}.
   *
   * @param server the proxy instance
   */
  public MemoryQueueManager(VelocityServer server) {
    super(server);

    this.queueCache = new MemoryQueueCache(server);
  }

  @Override
  public boolean isMasterProxy() {
    // note: MemoryQueueManager means there is only one proxy, so always true
    return true;
  }

  @Override
  public void pollFirst(final Queue queue, final QueuePlayer queuePlayer) {
    if (this.server.getPlayer(queuePlayer.getUniqueId()).isPresent()) {
      // Transfer the first player in the queue
      queue.transferFirst(queuePlayer);
    } else {
      // Remove the player from the queue if they are offline, yet still at the front
      queue.pollFirst();
    }
  }

  @Override
  public MemoryQueueCache getQueueCache() {
    return this.queueCache;
  }

  @Override
  public void broadcastMessage(Queue queue, Function<QueuePlayer, Component> component) {
    for (QueuePlayer queuePlayer : queue.getQueuePlayers()) {
      this.server.getPlayer(queuePlayer.getUniqueId()).ifPresent(player ->
          player.sendMessage(component.apply(queuePlayer)));
    }
  }

  @Override
  public void broadcastActionBar(Queue queue, Function<QueuePlayer, Component> component) {
    for (QueuePlayer queuePlayer : queue.getQueuePlayers()) {
      this.server.getPlayer(queuePlayer.getUniqueId()).ifPresent(player ->
          player.sendActionBar(component.apply(queuePlayer)));
    }
  }
}
