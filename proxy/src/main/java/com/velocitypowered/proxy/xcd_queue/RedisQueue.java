package com.velocitypowered.proxy.xcd_queue;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.xcd_queue.depot.QueueEntry;

/**
 * @author Elmar Blume - 29/10/2025
 */
public final class RedisQueue extends AbstractQueue {

  public RedisQueue(VelocityServer server, VelocityRegisteredServer backendInstance) {
    super(server, backendInstance);
  }

  public RedisQueue(VelocityServer server, VelocityRegisteredServer backendInstance, QueueEntry queueEntry) {
    super(server, backendInstance, queueEntry);
  }

  @Override
  protected void notifyMaxRetriesReached(Player player) {

  }
}
