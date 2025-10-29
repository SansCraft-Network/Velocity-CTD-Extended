package com.velocitypowered.proxy.xcd_queue.cache;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.xcd_queue.AbstractQueue;
import com.velocitypowered.proxy.xcd_queue.Queue;
import com.velocitypowered.proxy.xcd_queue.depot.QueueEntryDepot;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * @author Elmar Blume - 03/04/2025
 */
public final class RedisQueueCache implements QueueCache {

  private final VelocityServer server;
  private final QueueEntryDepot

  @Override
  public Queue getQueue(@NotNull String serverName) {
    return null;
  }

  @Override
  public Queue getQueue(@NotNull Player player) {
    return null;
  }

  @Override
  public Collection<Queue> getQueues() {
    return List.of();
  }

  @Override
  public void updateQueue(@NotNull Queue queue) {

  }
}
