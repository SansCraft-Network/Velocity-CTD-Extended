package com.velocitypowered.proxy.xcd_queue;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.xcd_queue.cache.MemoryQueueCache;

/**
 * @author Elmar Blume - 02/04/2025
 */
public final class MemoryQueueManager extends AbstractQueueManager<MemoryQueueCache> {

	public MemoryQueueManager(VelocityServer server) {
		super(server);
	}

	@Override
	public boolean isMasterProxy() {
		// note: MemoryQueueManager means there is only one proxy
		return true;
	}

	@Override
	public void tick() {

	}

	@Override
	public MemoryQueueCache getQueueCache() {
		return null;
	}
}
