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

package com.velocitypowered.proxy.queue;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.queue.QueueEntry;
import com.velocitypowered.api.queue.QueueEntryData;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.queue.redis.packet.VelocityQueueSync;
import com.velocitypowered.proxy.queue.redis.packet.VelocityQueueTransfer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player that is currently in a {@link VelocityQueue}.
 *
 * <p>Instances are created when a player is enqueued and destroyed when they are dequeued.
 * The {@code server} and {@code queue} fields are {@code transient} so they survive
 * JSON serialization for Redis persistence; they are re-injected via {@link #setContext}
 * after deserialization.</p>
 */
public final class VelocityQueueEntry implements QueueEntry {

  private final UUID uniqueId;
  private final String username;
  private volatile int priority;
  private volatile int connectionAttempts;
  private volatile boolean waitingForConnection;
  private volatile boolean fullBypass;
  private volatile boolean queueBypass;

  /**
   * Injected after construction or deserialization.
   */
  private transient VelocityServer server;

  /**
   * Injected after construction or deserialization.
   */
  private transient VelocityQueue queue;

  /**
   * Creates a new {@link VelocityQueueEntry} from the given data.
   *
   * @param server the proxy server
   * @param queue  the owning queue
   * @param data   the player data
   */
  public VelocityQueueEntry(final @NotNull VelocityServer server,
                            final @NotNull VelocityQueue queue,
                            final @NotNull QueueEntryData data) {
    this.server = server;
    this.queue = queue;
    this.uniqueId = data.uniqueId();
    this.username = data.username();
    this.priority = data.priority();
    this.fullBypass = data.fullBypass();
    this.queueBypass = data.queueBypass();
  }

  /**
   * Re-injects the server and queue context after deserialization from Redis.
   *
   * @param server the proxy server
   * @param queue  the owning queue
   */
  public void setContext(final @NotNull VelocityServer server, final @NotNull VelocityQueue queue) {
    this.server = server;
    this.queue = queue;
  }

  @Override
  public UUID getUniqueId() {
    return uniqueId;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public int getPriority() {
    return priority;
  }

  @Override
  public int getConnectionAttempts() {
    return connectionAttempts;
  }

  @Override
  public boolean isWaitingForConnection() {
    return waitingForConnection;
  }

  @Override
  public boolean isFullBypass() {
    return fullBypass;
  }

  @Override
  public boolean isQueueBypass() {
    return queueBypass;
  }

  @Override
  public VelocityQueue getQueue() {
    return queue;
  }

  /**
   * Initiates the transfer of this player to their target server.
   *
   * <p>In Redis mode the master proxy publishes a {@link VelocityQueueTransfer} packet
   * so the proxy that has the player connected can handle the actual connection.
   * A timeout task is scheduled to abort the transfer if no proxy handles it in time.
   * In memory mode the transfer is handled directly.</p>
   */
  public void transfer() {
    this.waitingForConnection = true;

    if (this.server.isRedisEnabled()) {
      publishWaitingChange();

      new VelocityQueueTransfer(this.uniqueId, this.queue.getName()).publish();

      this.server.getScheduler()
          .buildTask(VelocityVirtualPlugin.INSTANCE, this::abortTransfer)
          .delay(this.server.getConfiguration().getReadTimeout(), TimeUnit.MILLISECONDS)
          .schedule();
    } else {
      handleTransfer();
    }
  }

  /**
   * Performs the actual connection attempt for this player on the proxy that has them connected.
   * Called either directly (memory mode) or in response to a {@link VelocityQueueTransfer} packet
   * (Redis mode).
   */
  @ApiStatus.Internal
  public void handleTransfer() {
    final VelocityConfiguration.Queue config = this.server.getConfiguration().getQueue();
    final String targetServerName = this.queue.getName();

    this.server.getPlayer(this.uniqueId).ifPresentOrElse(player -> {
      final RegisteredServer foundServer = this.server.getServer(targetServerName).orElse(null);
      if (foundServer == null) {
        queue.dequeue(this.uniqueId);
        return;
      }

      final CompletableFuture<?> future = config.isForwardKickReason()
          ? player.createConnectionRequest(foundServer).connectWithIndication()
          : player.createConnectionRequest(foundServer).connect();

      future.thenAccept(result -> {
        boolean success = false;
        if (result instanceof Boolean b) {
          success = b;
        } else if (result instanceof ConnectionRequestBuilder.Result r) {
          success = r.isSuccessful();
        }

        if (success) {
          queue.dequeue(this.uniqueId);
        } else {
          resetAfterFailedTransfer(config);
        }
      }).exceptionally(ex -> {
        resetAfterFailedTransfer(config);
        return null;
      });
    }, () -> resetAfterFailedTransfer(config));
  }

  /**
   * Aborts a pending transfer that was never picked up by any proxy.
   *
   * <p>Called by the timeout task scheduled in {@link #transfer()} when Redis is enabled.
   * If the transfer was actually handled by another proxy, that proxy will have published a
   * {@code WAITING_CHANGE} sync (or a {@code DEQUEUE} sync) which updates the volatile
   * {@code waitingForConnection} field before this timeout fires - so the early-return
   * guard is always up to date without consulting the Redis depot.</p>
   */
  @ApiStatus.Internal
  public void abortTransfer() {
    // If the transfer was already handled (another proxy reset the flag via a WAITING_CHANGE
    // sync, or the entry was dequeued), the volatile field reflects that - skip the abort.
    if (!this.waitingForConnection) {
      return;
    }

    // Reset locally and notify other proxies
    this.waitingForConnection = false;
    this.connectionAttempts++;
    refreshPermissions();
    publishWaitingChange();
  }

  private void resetAfterFailedTransfer(final VelocityConfiguration.Queue config) {
    this.waitingForConnection = false;
    this.connectionAttempts++;
    refreshPermissions();
    publishWaitingChange();

    if (this.connectionAttempts >= config.getMaxSendRetries()) {
      final Component message = Component.translatable("velocity.queue.error.max-send-retries-reached")
          .arguments(
              Component.text(this.queue.getName()),
              Component.text(config.getMaxSendRetries()));
      this.server.getScheduler()
          .buildTask(VelocityVirtualPlugin.INSTANCE,
              () -> this.server.getPlayer(this.uniqueId).ifPresent(p -> p.sendMessage(message)))
          .delay(1, TimeUnit.SECONDS)
          .schedule();
      queue.dequeue(this.uniqueId);
    }
  }

  private void refreshPermissions() {
    this.server.getPlayer(this.uniqueId).ifPresent(player -> {
      this.priority = player.getQueuePriority(this.queue.getName());
      this.fullBypass = player.hasPermission("velocity.queue.full.bypass");
      this.queueBypass = player.hasPermission("velocity.queue.bypass");
    });
  }

  private void publishWaitingChange() {
    if (this.server.isRedisEnabled()) {
      new VelocityQueueSync(VelocityQueueSync.Payload.waitingChange(
          this.queue.getName(), this.uniqueId, this.waitingForConnection,
          this.connectionAttempts, this.priority, this.fullBypass, this.queueBypass
      )).publish();
    }
  }

  /**
   * Updates this entry's mutable fields from a WAITING_CHANGE sync packet.
   * Used when receiving a sync packet from another proxy.
   *
   * @param waiting            the new waitingForConnection value
   * @param attempts           the new connectionAttempts value
   * @param updatedPriority    the refreshed priority
   * @param updatedFullBypass  the refreshed fullBypass flag
   * @param updatedQueueBypass the refreshed queueBypass flag
   */
  @ApiStatus.Internal
  public void applyWaitingChangeFromPacket(final boolean waiting, final int attempts,
                                           final int updatedPriority,
                                           final boolean updatedFullBypass,
                                           final boolean updatedQueueBypass) {
    this.waitingForConnection = waiting;
    this.connectionAttempts = attempts;
    this.priority = updatedPriority;
    this.fullBypass = updatedFullBypass;
    this.queueBypass = updatedQueueBypass;
  }
}
