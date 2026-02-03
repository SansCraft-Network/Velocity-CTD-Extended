/*
 * Copyright (C) 2018-2026 Velocity Contributors
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
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
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
 */
public final class QueuePlayer {

  /**
   * The unique identifier of the player represented by this queue entry.
   */
  private final UUID uniqueId;

  /**
   * The username of the player represented by this queue entry.
   */
  private final String username;

  /**
   * The priority value used to order this player within the queue.
   * Higher values indicate higher priority.
   */
  private int priority;

  /**
   * The number of times a connection attempt has been made for this player.
   */
  private int connectionAttempts = 0;

  /**
   * Whether this player is currently in the process of connecting to a backend server.
   */
  private boolean waitingForConnection;

  /**
   * Whether this player is allowed to bypass full-server restrictions.
   */
  private boolean fullBypass;

  /**
   * Whether this player is allowed to bypass the queue entirely.
   */
  private boolean queueBypass;

  /**
   * The proxy server instance associated with this queue player.
   * Marked {@code transient} as it is context-dependent and not serialized.
   */
  private transient VelocityServer server;

  /**
   * The queue that currently owns this player.
   * Marked {@code transient} as it is reconstructed from context.
   */
  private transient Queue queue;

  /**
   * The target backend server instance this player is queued to join.
   * Marked {@code transient} as it is derived from the owning queue.
   */
  private transient VelocityRegisteredServer targetInstance;

  /**
   * Creates a new {@link QueuePlayer}.
   *
   * @param server the proxy instance
   * @param queue  the queue instance
   * @param data   the backing data for this queue player, including identifiers,
   *               priority, and bypass flags
   */
  public QueuePlayer(final @NotNull VelocityServer server, final @NotNull Queue queue,
                     final @NotNull QueuePlayerData data) {
    this.server = server;
    this.queue = queue;
    this.targetInstance = queue.getBackendInstance();

    this.uniqueId = data.uniqueId();
    this.username = data.username();

    this.priority = data.priority();
    this.fullBypass = data.fullBypass();
    this.queueBypass = data.queueBypass();
  }

  /**
   * Initiates the transfer process for the player represented by this {@code QueuePlayer} instance.
   *
   * <p>If Redis support is enabled in the server configuration, the transfer is handled by publishing
   * a {@code VelocityQueueTransfer} packet. Otherwise, a direct transfer process is
   * initiated by calling {@code handleTransfer}.</p>
   *
   * <p>During the transfer process, the player's state is marked as {@code waitingForConnection},
   * preventing potential conflicts from multiple transfer attempts.</p>
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
    if (this.targetInstance == null) {
      throw new IllegalStateException("Tried to transfer queue player, while target instance is null");
    }

    final VelocityConfiguration.Queue config = this.server.getConfiguration().getQueue();
    this.server.getPlayer(this.getUniqueId()).ifPresent(player -> {
      final String targetServerName = this.targetInstance.getServerInfo().getName();
      final Queue queue = this.server.getQueueManager().getQueueCache().getQueue(targetServerName);

      final RegisteredServer foundServer = this.server.getServer(targetServerName).orElse(null);
      if (foundServer == null) {
        queue.dequeue(player.getUniqueId(), false);
        return;
      }

      CompletableFuture<?> future;
      if (config.isForwardKickReason()) {
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
          queue.dequeue(this.uniqueId, false);
        } else {
          updateProperties();

          if (this.connectionAttempts == config.getMaxSendRetries()) {
            queue.dequeue(this.uniqueId, true);
          }
        }
      }).exceptionally(ex -> {
        updateProperties();

        if (this.connectionAttempts == config.getMaxSendRetries()) {
          queue.dequeue(this.uniqueId, true);
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
  public void copyFrom(final @NotNull QueuePlayer other) {
    this.priority = other.priority;
    this.connectionAttempts = other.connectionAttempts;
    this.waitingForConnection = other.waitingForConnection;
    this.fullBypass = other.fullBypass;
    this.queueBypass = other.queueBypass;
  }

  /**
   * Sets the context for this {@code QueuePlayer} instance with the specified server and queue.
   *
   * @param server the VelocityServer instance
   * @param queue  the Queue instance
   */
  public void setContext(final @NotNull VelocityServer server, final @NotNull Queue queue) {
    this.server = server;
    this.queue = queue;
    this.targetInstance = queue.getBackendInstance();
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
