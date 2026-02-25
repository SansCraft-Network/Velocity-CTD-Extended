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

package com.velocitypowered.proxy.queue.manager;

import static com.velocitypowered.proxy.queue.model.QueueState.ACTIVE;
import static com.velocitypowered.proxy.queue.model.QueueState.PAUSED;
import static com.velocitypowered.proxy.queue.model.ServerStatus.FULL;
import static com.velocitypowered.proxy.queue.model.ServerStatus.OFFLINE;
import static com.velocitypowered.proxy.queue.model.ServerStatus.ONLINE;
import static com.velocitypowered.proxy.queue.model.ServerStatus.WAITING;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.queue.AbstractQueue;
import com.velocitypowered.proxy.queue.Queue;
import com.velocitypowered.proxy.queue.cache.QueueCache;
import com.velocitypowered.proxy.queue.model.QueuePlayer;
import com.velocitypowered.proxy.queue.model.ServerStatus;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an abstraction of {@link QueueManager} which is used in
 * the {@link MemoryQueueManager Memory} or {@link RedisQueueManager Redis} implementations.
 *
 * @param <C> the type of {@link QueueCache} backing this queue manager
 * @see MemoryQueueManager
 * @see RedisQueueManager
 */
public abstract sealed class AbstractQueueManager<C extends QueueCache> implements QueueManager<C>
    permits MemoryQueueManager, RedisQueueManager {

  /**
   * Tracks the timestamp (in milliseconds) of when each server was last marked as ONLINE.
   * Used to implement queue delay logic for transitioning servers from WAITING to ONLINE.
   */
  protected static final Map<String, Long> LAST_TURNED_ONLINE_TIME = new ConcurrentHashMap<>();

  /**
   * The proxy server instance that owns this queue manager.
   */
  protected final VelocityServer server;

  /**
   * Scheduled task responsible for periodically transferring players from queues.
   */
  private ScheduledTask transferTask;

  /**
   * Scheduled task responsible for periodically sending action bar updates to queued players.
   */
  private ScheduledTask actionBarTask;

  /**
   * Scheduled task responsible for periodically pinging backend servers to update their status.
   */
  private ScheduledTask backendHandshakeTask;

  /**
   * Constructs a new {@link AbstractQueueManager}.
   *
   * @param server the proxy instance
   */
  public AbstractQueueManager(final @NotNull VelocityServer server) {
    this.server = server;

    this.rescheduleTasks();
  }

  /**
   * Reloads the queue manager configuration and reschedules internal tasks
   * based on the latest proxy settings.
   */
  @Override
  public void reload() {
    this.rescheduleTasks();
  }

  /**
   * Tears down this queue manager by cancelling scheduled tasks and
   * invoking {@link Queue#teardown()} on all managed queues.
   */
  @Override
  public void teardown() {
    if (this.transferTask != null) {
      this.transferTask.cancel();
    }

    if (this.actionBarTask != null) {
      this.actionBarTask.cancel();
    }

    if (this.backendHandshakeTask != null) {
      this.backendHandshakeTask.cancel();
    }

    for (Queue queue : this.getQueueCache().getQueues()) {
      queue.teardown();
    }
  }

  /**
   * Enqueues the given player for the specified backend server, applying
   * queue configuration rules such as bypass, multi-queue restrictions,
   * paused-queue behavior, and version compatibility checks.
   *
   * @param player        the player to enqueue
   * @param targetBackend the backend server the player is attempting to join
   */
  @Override
  public void queue(final Player player, final VelocityRegisteredServer targetBackend) {
    final String targetBackendName = targetBackend.getServerInfo().getName();
    final Queue queue = this.getQueueCache().getQueue(targetBackendName);

    final VelocityConfiguration.Queue config = this.server.getConfiguration().getQueue();
    if (!config.isEnabled() || player.hasPermission("velocity.queue.bypass")) {
      player.createConnectionRequest(targetBackend).connectWithIndication();
      return;
    }

    if (queue.contains(player)) {
      player.sendMessage(Component.translatable("velocity.queue.error.already-queued")
          .arguments(Component.text(targetBackendName)));
      return;
    }

    if (!config.isAllowMultiQueue()) {
      for (Queue iqueue : this.getQueueCache().getQueues()) {
        if (iqueue.contains(player)) {
          iqueue.dequeue(player, false);
          player.sendMessage(Component.translatable("velocity.queue.error.queued-swap").arguments(
              Argument.string("from", iqueue.getName()), Argument.string("to", targetBackendName)));
          break;
        }
      }
    }

    if (queue.getState() == PAUSED && !config.isAllowPausedQueueJoining()) {
      player.sendMessage(Component.translatable("velocity.queue.error.paused")
          .arguments(Component.text(targetBackendName)));
      return;
    }

    if (player instanceof ConnectedPlayer connectedPlayer && connectedPlayer.checkVersionCompatibility(targetBackend)) {
      return;
    }

    queue.enqueue(player);
    player.sendMessage(Component.translatable("velocity.queue.command.queued")
        .arguments(Component.text(targetBackendName)));
  }

  @Override
  public final void onPlayerDisconnect(final Player player) {
    final long timeout = getTimeoutInSeconds(player);

    if (timeout == -1 || this.server.isShuttingDown()) {
      removePlayerEntirely(player);
    } else {
      this.server.getScheduler().buildTask(VelocityVirtualPlugin.INSTANCE, () ->
          removePlayerEntirely(player)).delay(timeout, TimeUnit.SECONDS).schedule();
    }
  }

  /**
   * Removes the given player from all queues managed by this queue manager.
   *
   * @param player the player to remove from all queues
   */
  @Override
  public void removePlayerEntirely(final Player player) {
    for (Queue queue : this.getQueueCache().getQueues()) {
      if (queue.contains(player)) {
        queue.dequeue(player, false);
      }
    }
  }

  /**
   * Reschedules various tasks related to the queue system, ensuring they run
   * only if the current proxy is designated as the master proxy.
   *
   * <ul>
   *   <li>Schedules the task responsible for transferring players.</li>
   *   <li>Schedules the task handling backend server handshakes.</li>
   *   <li>Schedules the task for broadcasting action bar messages to players in queues.</li>
   * </ul>
   *
   * <p>If the current proxy is not the master proxy, the method returns without performing
   * any action.</p>
   */
  private void rescheduleTasks() {
    if (!this.isMasterProxy()) {
      return;
    }

    this.scheduleTransferTask();
    this.scheduleBackendHandshakeTask();
    this.scheduleActionBarTask();
  }

  /**
   * Schedules the task responsible for transferring players.
   */
  private void scheduleTransferTask() {
    if (this.transferTask != null) {
      this.transferTask.cancel();
    }

    final VelocityConfiguration.Queue config = this.server.getConfiguration().getQueue();
    this.transferTask = this.server.getScheduler()
        .buildTask(VelocityVirtualPlugin.INSTANCE, this::transfer)
        .repeat((long) (config.getSendDelay() * 1000), TimeUnit.MILLISECONDS)
        .schedule();
  }

  /**
   * Schedules the task responsible for handling backend server handshakes.
   */
  private void scheduleBackendHandshakeTask() {
    if (this.backendHandshakeTask != null) {
      this.backendHandshakeTask.cancel();
    }

    final VelocityConfiguration.Queue config = this.server.getConfiguration().getQueue();
    this.backendHandshakeTask = this.server.getScheduler()
        .buildTask(VelocityVirtualPlugin.INSTANCE, this::pingBackends)
        .delay((long) (config.getBackendPingInterval() * 1000), TimeUnit.MILLISECONDS)
        .repeat((long) (config.getBackendPingInterval() * 1000), TimeUnit.MILLISECONDS)
        .schedule();
  }

  /**
   * Schedules the task responsible for broadcasting action bar messages to players in queues.
   */
  private void scheduleActionBarTask() {
    if (this.actionBarTask != null) {
      this.actionBarTask.cancel();
    }

    final VelocityConfiguration.Queue config = this.server.getConfiguration().getQueue();
    this.actionBarTask = this.server.getScheduler()
        .buildTask(VelocityVirtualPlugin.INSTANCE, () -> {
          for (Queue queue : this.getQueueCache().getQueues()) {
            this.broadcastActionBar(queue, player -> ((AbstractQueue) queue).createActionbarComponent(player));
          }
        })
        .delay((long) (config.getMessageDelay() * 1000), TimeUnit.MILLISECONDS)
        .repeat((long) (config.getMessageDelay() * 1000), TimeUnit.MILLISECONDS)
        .schedule();
  }

  /**
   * Handles the logic for transferring players from active queues if the current proxy
   * is designated as the master proxy. The method processes up to a maximum of 10 queues
   * at a time to avoid overwhelming the system.
   *
   * <p>The transfer process adheres to the following conditions:</p>
   *
   * <p>- Only queues in the {@code QueueState#ACTIVE} state are considered.
   *    - Only queues where the backend server status is {@code ServerStatus#ONLINE} are processed.
   *    - Queues with a size greater than 0 are eligible for processing.</p>
   *
   * <p>For each eligible queue:
   * - The first {@link QueuePlayer} in the queue is retrieved.
   * - The player will not be transferred if the server is full, unless the player has a full bypass flag.
   * - If the conditions are met, the player is transferred using the {@code pollFirst} method.</p>
   *
   * <p>If the current proxy is not a master proxy, the method exits without performing any operation.</p>
   */
  private void transfer() {
    if (!this.isMasterProxy() || this.getQueueCache() == null) {
      return;
    }

    Set<UUID> transferred = new HashSet<>();
    getQueueCache().getQueues().stream()
        .filter(queue -> queue.getState() == ACTIVE)
        .filter(queue -> queue.getServerStatus().isActive())
        .filter(queue -> queue.size() > 0)
        .limit(10)
        .forEach(queue -> {
          final QueuePlayer queuePlayer = queue.getQueuePlayers().stream().findFirst().orElse(null);
          if (queuePlayer == null
                  || (queue.getServerStatus() == FULL && !queuePlayer.isFullBypass())
                  || transferred.contains(queuePlayer.getUniqueId())) {
            return;
          }

          this.pollFirst(queue, queuePlayer);

          transferred.add(queuePlayer.getUniqueId());
        });
  }

  /**
   * Pings backend servers to determine their availability and updates the status of
   * associated queues accordingly. This method processes only a limited number of servers
   * to avoid overwhelming the system.
   *
   * <p>Status transitions driven by each ping result:</p>
   * <ul>
   *   <li>Ping fails → {@code OFFLINE}.</li>
   *   <li>Ping succeeds, was {@code OFFLINE} → {@code WAITING}; warmup timer starts.</li>
   *   <li>Ping succeeds, already {@code ONLINE} or {@code FULL} → {@code ONLINE} or {@code FULL}
   *       based on the server's self-reported player count (kept continuously in sync).</li>
   *   <li>Ping succeeds, still {@code WAITING}, warmup delay elapsed → {@code ONLINE} or
   *       {@code FULL} based on player count.</li>
   * </ul>
   *
   * <p>{@code FULL} is set when the server's self-reported online count equals or exceeds its
   * maximum capacity; {@code ONLINE} is set otherwise. If the server does not report player
   * counts the status defaults to {@code ONLINE}.</p>
   */
  private void pingBackends() {
    if (this.getQueueCache() == null) {
      return;
    }

    this.getQueueCache().getQueues().stream()
        .limit(5)
        .forEach(queue -> {
          RegisteredServer s = this.server.getServer(queue.getName()).orElse(null);
          if (s == null) {
            return;
          }

          s.ping().orTimeout(3, TimeUnit.SECONDS).whenComplete((result, th) -> {
            if (th != null) {
              queue.setServerStatus(OFFLINE);
              return;
            }

            // Determine active status from the server's self-reported player count.
            // If the server does not report player counts, default to ONLINE.
            final boolean serverFull = result.getPlayers()
                .map(players -> players.getMax() > 0 && players.getOnline() >= players.getMax())
                .orElse(false);
            final ServerStatus activeStatus = serverFull ? FULL : ONLINE;

            // Server was offline - start the warmup period before sending players.
            if (queue.getServerStatus() == OFFLINE) {
              queue.setServerStatus(WAITING);
              LAST_TURNED_ONLINE_TIME.put(queue.getName(), System.currentTimeMillis());
            }

            // Already in an active state - keep ONLINE/FULL in sync with player count.
            if (queue.getServerStatus().isActive()) {
              queue.setServerStatus(activeStatus);
              return;
            }

            // Still in WAITING - check whether the warmup delay has elapsed.
            final Long lastOnlineTime = LAST_TURNED_ONLINE_TIME.get(queue.getName());
            if (lastOnlineTime != null) {
              final double queueDelay = this.server.getConfiguration().getQueue().getQueueDelay() * 1000;
              if (System.currentTimeMillis() >= lastOnlineTime + queueDelay) {
                queue.setServerStatus(activeStatus);
              }
            }
          }).exceptionally(throwable -> {
            queue.setServerStatus(OFFLINE);
            return null;
          });
        });
  }

  /**
   * Gets the timeout in seconds at which the player will be removed from a queue
   * after leaving the server.
   *
   * @param player The player to get the timeout for.
   * @return The timeout in seconds at which the player will be removed from a queue after leaving the server.
   */
  private int getTimeoutInSeconds(final Player player) {
    if (player.hasPermission("velocity.queue.timeout.exempt")) {
      return 0;
    }

    for (int i = 86400; i > 0; i--) {
      if (player.hasPermission("velocity.queue.timeout." + i)) {
        return i;
      }
    }

    return -1;
  }
}
