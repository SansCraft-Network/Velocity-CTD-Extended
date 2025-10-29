package com.velocitypowered.proxy.xcd_queue;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;

/**
 * @author Elmar Blume - 29/10/2025
 */
public final class RedisQueue extends AbstractQueue {

  /**
   * Constructs a new {@link java.util.AbstractQueue}
   *
   * @param server          the proxy server instance
   * @param backendInstance the backend instance server
   */
  public RedisQueue(VelocityServer server, VelocityRegisteredServer backendInstance) {
    super(server, backendInstance);
  }

  @Override
  protected void notifyMaxRetriesReached(Player player) {

  }
}
