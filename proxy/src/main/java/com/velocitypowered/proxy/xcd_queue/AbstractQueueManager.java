package com.velocitypowered.proxy.xcd_queue;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.xcd_queue.cache.QueueCache;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an abstraction of {@link QueueManager} which is used in
 * the {@link MemoryQueueManager Memory} or {@link RedisQueueManager Redis} implementations
 *
 * @author Elmar Blume - 02/04/2025
 * @see MemoryQueueManager
 * @see RedisQueueManager
 */
public sealed abstract class AbstractQueueManager<C extends QueueCache> implements QueueManager<C>
		permits MemoryQueueManager, RedisQueueManager {

	protected final VelocityServer proxy;
	protected VelocityConfiguration.Queue config;

	/**
	 * Constructs a new {@link AbstractQueueManager}
	 *
	 * @param proxy the proxy instance
	 */
	public AbstractQueueManager(@NotNull VelocityServer proxy) {
		this.proxy = proxy;
		this.config = proxy.getConfiguration().getQueue();
	}
}
