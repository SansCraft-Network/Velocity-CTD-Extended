package com.velocitypowered.proxy.xcd_queue.manager;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.xcd_queue.Queue;
import com.velocitypowered.proxy.xcd_queue.cache.MemoryQueueCache;
import com.velocitypowered.proxy.xcd_queue.AbstractQueue;
import com.velocitypowered.proxy.xcd_queue.model.QueuePlayer;
import net.kyori.adventure.text.Component;

import java.util.function.Function;

/**
 * @author Elmar Blume - 02/04/2025
 */
public final class MemoryQueueManager extends AbstractQueueManager<MemoryQueueCache> {

  private final MemoryQueueCache queueCache;

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
