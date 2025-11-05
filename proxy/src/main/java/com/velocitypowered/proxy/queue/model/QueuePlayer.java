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

package com.velocitypowered.proxy.queue.model;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.queue.AbstractQueue;
import com.velocitypowered.proxy.queue.Queue;
import com.velocitypowered.proxy.queue.redis.packet.VelocityQueueTransfer;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player in a {@link AbstractQueue}.
 *
 * @author Elmar Blume - 03/04/2025
 */
public final class QueuePlayer {

  private final transient VelocityServer server;
  private final transient Queue queue;
  private final transient VelocityRegisteredServer targetInstance;

  private final UUID uniqueId;
  private final String username;

  private int priority;
  private int connectionAttempts = 0;
  private boolean waitingForConnection;
  private boolean fullBypass;
  private boolean queueBypass;

  /**
   * Creates a new {@link QueuePlayer}.
   *
   * @param server the proxy instance
   * @param player the player instance
   * @param queue  the queue instance
   */
  public QueuePlayer(final VelocityServer server, final Player player, final Queue queue) {
    this.server = server;
    this.queue = queue;
    this.targetInstance = queue.getBackendInstance();

    this.uniqueId = player.getUniqueId();
    this.username = player.getUsername();

    // Setup configurable properties
    this.priority = player.getQueuePriority(queue.getName());
    this.fullBypass = player.hasPermission("velocity.queue.full.bypass");
    this.queueBypass = player.hasPermission("velocity.queue.bypass");
  }

  /**
   * Initiates the transfer process for the player represented by this {@code QueuePlayer} instance.
   *
   * <p>
   * If Redis support is enabled in the server configuration, the transfer is handled by publishing
   * a {@code VelocityQueueTransfer} packet. Otherwise, a direct transfer process is
   * initiated by calling {@code handleTransfer}.
   *
   * <p>
   * During the transfer process, the player's state is marked as {@code waitingForConnection},
   * preventing potential conflicts from multiple transfer attempts.
   */
  public void transfer() {
    this.waitingForConnection = true;

    if (this.server.isRedisEnabled()) {
      new VelocityQueueTransfer(this.getUniqueId(), this.targetInstance.getServerInfo().getName())
          .publish();
    } else {
      handleTransfer();
    }
  }

  /**
   * Handles the transfer process for a {@link QueuePlayer} to the target server.
   */
  @ApiStatus.Internal
  public void handleTransfer() {
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
              .dequeue(this.uniqueId, false);
        } else {
          updateProperties();

          if (getConnectionAttempts() == this.server.getConfiguration().getQueue().getMaxSendRetries()) {
            server.getQueueManager().getQueueCache().getQueue(targetServerName)
                .dequeue(this.uniqueId, true);
          }
        }
      }).exceptionally(ex -> {
        updateProperties();

        if (getConnectionAttempts() == this.server.getConfiguration().getQueue().getMaxSendRetries()) {
          server.getQueueManager().getQueueCache().getQueue(targetServerName)
              .dequeue(this.uniqueId, true);
        }

        return null;
      });
    });
  }

  /**
   * Copies the state and properties from the provided {@code QueuePlayer} instance to this instance.
   *
   * @param other the {@code QueuePlayer} instance from which to copy the properties; must not be null
   */
  public void copyFrom(@NotNull QueuePlayer other) {
    this.priority = other.priority;
    this.connectionAttempts = other.connectionAttempts;
    this.waitingForConnection = other.waitingForConnection;
    this.fullBypass = other.fullBypass;
    this.queueBypass = other.queueBypass;
  }

  /**
   * Updates the properties of this {@code QueuePlayer} instance based on the current state.
   */
  private void updateProperties() {
    this.waitingForConnection = false;
    this.connectionAttempts++;

    this.server.getPlayer(this.uniqueId).ifPresent(player -> {
      this.fullBypass = player.hasPermission("velocity.queue.full.bypass");
      this.queueBypass = player.hasPermission("velocity.queue.bypass");
      this.priority = player.getQueuePriority(this.queue.getName());
    });

    if (this.server.isRedisEnabled()) {
      this.server.getRedis().getQueueService().upsertQueuePlayer(this);
      //todo check if it works
    }
  }

  /**
   * Retrieves the name of the queue associated with this {@code QueuePlayer} instance.
   *
   * @return the name of the queue
   */
  public String getQueueName() {
    return this.queue.getName();
  }

  /**
   * Retrieves the username of the player represented by this {@code QueuePlayer} instance.
   *
   * @return the username of the player
   */
  public String getUsername() {
    return username;
  }

  /**
   * Retrieves the unique identifier of the player represented by this {@code QueuePlayer} instance.
   *
   * @return the unique identifier of the player
   */
  public UUID getUniqueId() {
    return this.uniqueId;
  }

  /**
   * Retrieves the priority of the player represented by this {@code QueuePlayer} instance.
   *
   * @return the priority of the player
   */
  public int getPriority() {
    return priority;
  }

  /**
   * Checks if the player has bypassed the queue.
   *
   * @return true if the player has bypassed the queue, false otherwise
   */
  public boolean isQueueBypass() {
    return queueBypass;
  }

  /**
   * Checks if the player has bypassed the server's full capacity.
   *
   * @return true if the player has bypassed the server's full capacity, false otherwise
   */
  public boolean isFullBypass() {
    return fullBypass;
  }

  /**
   * Checks if the player is currently waiting for a connection.
   *
   * @return true if the player is waiting for a connection, false otherwise
   */
  public boolean isWaitingForConnection() {
    return waitingForConnection;
  }

  /**
   * Retrieves the number of connection attempts made by the player.
   *
   * @return the number of connection attempts
   */
  public int getConnectionAttempts() {
    return connectionAttempts;
  }
}
