package com.velocitypowered.proxy.xcd_queue.manager;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.xcd_queue.Queue;
import com.velocitypowered.proxy.xcd_queue.cache.QueueCache;
import com.velocitypowered.proxy.xcd_queue.AbstractQueue;
import com.velocitypowered.proxy.xcd_queue.model.QueuePlayer;
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

  boolean isMasterProxy();

  void pollFirst(final Queue queue, final QueuePlayer queuePlayer);

  void queue(final Player player, final VelocityRegisteredServer server);

  C getQueueCache();

  void onPlayerDisconnect(final Player player);

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
