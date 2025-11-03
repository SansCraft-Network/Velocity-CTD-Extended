package com.velocitypowered.proxy.xcd_queue.model;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.xcd_queue.AbstractQueue;
import com.velocitypowered.proxy.xcd_queue.Queue;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a player in a {@link AbstractQueue}
 *
 * @author Elmar Blume - 03/04/2025
 */
public final class QueuePlayer {

  private transient final VelocityServer server;
  private transient final Queue queue;
  private transient final VelocityRegisteredServer targetInstance;

  private final String username;

  private int priority, connectionAttempts = 0;
  private boolean waitingForConnection, fullBypass, queueBypass;

  private transient Player player;

  public QueuePlayer(final VelocityServer server, final Player player, final Queue queue) {
    this.server = server;
    this.player = player;
    this.queue = queue;
    this.targetInstance = queue.getBackendInstance();
    this.username = player.getUsername();

    // Setup configurable properties
    this.priority = player.getQueuePriority(queue.getName());
    this.fullBypass = player.hasPermission("velocity.queue.full.bypass");
    this.queueBypass = player.hasPermission("velocity.queue.bypass");
  }

  public void transfer() {
    this.waitingForConnection = true;

    if (this.server.isRedisEnabled()) {
      //todo send queuetransfer packet
    } else {
      handleTransfer();
    }
  }

  private void handleTransfer() {
    this.server.getPlayer(this.getUniqueId()).ifPresent(player -> {
      final String targetServerName = targetInstance.getServerInfo().getName();
      RegisteredServer foundServer = this.server.getServer(targetServerName).orElse(null);

      if (foundServer == null) {
        this.server.getQueueManager().getQueueCache().getQueue(targetServerName)
                .dequeue(player.getUniqueId(), false);
        return;
      }

      CompletableFuture<?> future;
      if (this.server.getConfiguration().getQueue().isForwardKickReason()) {
        future = player.createConnectionRequest(foundServer).connectWithIndication();
      } else {
        future = player.createConnectionRequest(foundServer).connect();
      }

      future.thenAccept(result -> {
        boolean success = false;
        if (result instanceof Boolean b) {
          success = b;
        } else if (result instanceof ConnectionRequestBuilder.Result s) {
          success = s.isSuccessful();
        }

        if (success) {
          server.getQueueManager().getQueueCache().getQueue(targetServerName)
                  .dequeue(this.player, false);
        } else {
          updateProperties();

          if (getConnectionAttempts() == this.server.getConfiguration().getQueue().getMaxSendRetries()) {
            server.getQueueManager().getQueueCache().getQueue(targetServerName)
                    .dequeue(this.player, true);
          }
        }
      }).exceptionally(ex -> {
        updateProperties();

        if (getConnectionAttempts() == this.server.getConfiguration().getQueue().getMaxSendRetries()) {
          server.getQueueManager().getQueueCache().getQueue(targetServerName)
                  .dequeue(this.player, true);
        }

        return null;
      });
    });
  }

  public void copyFrom(@NotNull QueuePlayer other) {
    this.priority = other.priority;
    this.connectionAttempts = other.connectionAttempts;
    this.waitingForConnection = other.waitingForConnection;
    this.fullBypass = other.fullBypass;
    this.queueBypass = other.queueBypass;
  }

  private void updateProperties() {
    this.waitingForConnection = false;
    this.connectionAttempts++;

    if (this.player == null) {
      this.player = this.server.getPlayer(this.getUniqueId()).orElse(null);
    }

    if (this.player != null) {
      this.fullBypass = this.player.hasPermission("velocity.queue.full.bypass");
      this.queueBypass = this.player.hasPermission("velocity.queue.bypass");
      this.priority = this.player.getQueuePriority(this.queue.getName());
    }

    if (this.server.isRedisEnabled()) {
      this.server.getRedis().getQueueService().upsertQueuePlayer(this);//todo check if it works
    }
  }

  public String getQueueName() {
    return this.queue.getName();
  }

  public String getUsername() {
    return username;
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
