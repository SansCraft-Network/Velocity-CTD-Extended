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
import com.velocitypowered.proxy.queue.cache.RedisQueueCache;
import com.velocitypowered.proxy.queue.model.QueuePlayer;
import com.velocitypowered.proxy.redis.impl.depot.PlayerDepotService;
import com.velocitypowered.proxy.redis.impl.packet.VelocityActionBar;
import com.velocitypowered.proxy.redis.impl.packet.VelocityMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the redis implementation of {@link QueueManager} which uses a {@link RedisQueueCache}
 *
 * @author Elmar Blume - 02/04/2025
 */
public final class RedisQueueManager extends AbstractQueueManager<RedisQueueCache> {

  private final RedisQueueCache queueCache;
  private final PlayerDepotService playerService;

  /**
   * Constructs a new {@link RedisQueueManager}
   *
   * @param server the proxy instance
   */
  public RedisQueueManager(@NotNull VelocityServer server) {
    super(server);

    this.queueCache = new RedisQueueCache(server);
    this.playerService = server.getRedis().getPlayerService();
  }

  @Override
  public boolean isMasterProxy() {
    final List<String> masterProxies = this.server.getConfiguration().getQueue().getMasterProxyIds();
    final List<String> activeProxies = new ArrayList<>(this.server.getRedis().getProxyService()
            .getAllProxyIds().stream().toList());
    Collections.sort(activeProxies);

    final String ownProxy = this.server.getProxyId();

    // If no master proxies are configured but there's only one active proxy, use it as master
    if (masterProxies.isEmpty() || (masterProxies.size() == 1 && masterProxies.getFirst().isEmpty())) {
      if (activeProxies.size() == 1 && activeProxies.getFirst().equalsIgnoreCase(ownProxy)) {
        return true;
      }
      // If there's only one active proxy, use it as master regardless of configuration
      if (activeProxies.size() == 1) {
        return activeProxies.getFirst().equalsIgnoreCase(ownProxy);
      }
    }

    activeProxies.retainAll(masterProxies);

    String firstMasterProxy = null;

    for (String activeProxy : activeProxies) {
      if (masterProxies.contains(activeProxy)) {
        firstMasterProxy = activeProxy;
        break;
      }
    }

    if (firstMasterProxy != null && firstMasterProxy.equalsIgnoreCase(ownProxy)) {
      int ownIndex = masterProxies.indexOf(ownProxy);
      int firstIndex = masterProxies.indexOf(firstMasterProxy);

      return ownIndex != -1 && ownIndex == firstIndex;
    }

    return false;
  }

  @Override
  public void pollFirst(final Queue queue, final QueuePlayer queuePlayer) {
    if (this.playerService.isPlayerOnline(queuePlayer.getUniqueId())) {
      // Transfer the first player in the queue
      queue.transferFirst(queuePlayer);
    } else {
      // Remove the player from the queue if they are offline, yet still at the front
      queue.pollFirst();
    }

    // Update the queue in the cache
    this.queueCache.updateQueue(queue);
  }

  @Override
  public RedisQueueCache getQueueCache() {
    return this.queueCache;
  }

  @Override
  public void broadcastMessage(Queue queue, Function<QueuePlayer, Component> component) {
    for (QueuePlayer queuePlayer : queue.getQueuePlayers()) {
      new VelocityMessage(queuePlayer.getUniqueId(), component.apply(queuePlayer))
              .publish();
    }
  }

  @Override
  public void broadcastActionBar(Queue queue, Function<QueuePlayer, Component> component) {
    for (QueuePlayer queuePlayer : queue.getQueuePlayers()) {
      new VelocityActionBar(queuePlayer.getPlayer(), component.apply(queuePlayer))
              .publish();
    }
  }
}
