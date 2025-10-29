package com.velocitypowered.proxy.xcd_queue.model;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.xcd_queue.AbstractQueue;
import com.velocitypowered.proxy.xcd_queue.Queue;

import java.util.UUID;

/**
 * Represents a player in a {@link AbstractQueue}
 *
 * @author Elmar Blume - 03/04/2025
 */
public final class QueuePlayer {

  private transient final VelocityServer server;

  private transient final Player player;
  private transient final VelocityRegisteredServer targetInstance;

  private int priority, connectionAttempts = 0;
  private boolean waitingForConnection, fullBypass, queueBypass;

  public QueuePlayer(final VelocityServer server, final Player player, final Queue queue) {
    this.server = server;
    this.player = player;
    this.targetInstance = queue.getBackendInstance();

    // Setup configurable properties
    this.priority = player.getQueuePriority(queue.getName());
    this.fullBypass = player.hasPermission("velocity.queue.full.bypass");
    this.queueBypass = player.hasPermission("velocity.queue.bypass");
  }

  public void transfer() {

  }

  public Player getPlayer() {
    return player;
  }

  public UUID getUniqueId() {
    return this.player.getUniqueId();
  }

  public int getPriority() {
    return priority;
  }

  public boolean isQueueBypass() {
    return queueBypass;
  }

  public boolean isFullBypass() {
    return fullBypass;
  }

  public boolean isWaitingForConnection() {
    return waitingForConnection;
  }

  public int getConnectionAttempts() {
    return connectionAttempts;
  }
}
