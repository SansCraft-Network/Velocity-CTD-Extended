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

package com.velocitypowered.proxy.queue.manager;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.queue.AbstractQueue;
import com.velocitypowered.proxy.queue.Queue;
import com.velocitypowered.proxy.queue.cache.QueueCache;
import com.velocitypowered.proxy.queue.model.QueuePlayer;
import com.velocitypowered.proxy.queue.model.QueueState;
import com.velocitypowered.proxy.queue.model.ServerStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Represents an abstraction of {@link QueueManager} which is used in
 * the {@link MemoryQueueManager Memory} or {@link RedisQueueManager Redis} implementations
 *
 * @author Elmar Blume - 02/04/2025
 * @see MemoryQueueManager
 * @see RedisQueueManager
 */
public sealed abstract class AbstractQueueManager<C extends QueueCache> implements QueueManager<C>
        permits MemoryQueueManager, RedisQueueManager {

  /**
   * Tracks the timestamp (in milliseconds) of when each server was last marked as ONLINE.
   * Used to implement queue delay logic for transitioning servers from WAITING to ONLINE.
   */
  protected static final Map<String, Long> LAST_TURNED_ONLINE_TIME = new ConcurrentHashMap<>();

  protected final VelocityServer server;
  private ScheduledTask transferTask, actionBarTask, backendHandshakeTask;

  /**
   * Constructs a new {@link AbstractQueueManager}
   *
   * @param server the proxy instance
   */
  public AbstractQueueManager(@NotNull final VelocityServer server) {
    this.server = server;

    // Schedule the tasks
    this.rescheduleTasks();
  }

  @Override
  public void reload() {
    // Reschedule tasks
    this.rescheduleTasks();
  }

  @Override
  public void queue(final Player player, final VelocityRegisteredServer targetBackend) {
    final String targetBackendName = targetBackend.getServerInfo().getName();
    final Queue queue = this.getQueueCache().getQueue(targetBackendName);
    if (queue == null) {
      throw new IllegalArgumentException("No queue found for target backend: " + targetBackendName);
    }

    final VelocityConfiguration.Queue config = this.server.getConfiguration().getQueue();
    if (!config.isEnabled() || player.hasPermission("velocity.queue.bypass")) {
      player.createConnectionRequest(targetBackend).connectWithIndication();
      return;
    }

    if (queue.contains(player)) {
      player.sendMessage(Component.translatable("velocity.queue.error.already-queued")
              .arguments(Argument.string("server", targetBackendName)));
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

    if (queue.isPaused() && !config.isAllowPausedQueueJoining()) {
      player.sendMessage(Component.translatable("velocity.queue.error.paused")
              .arguments(Argument.string("server", targetBackendName)));
      return;
    }

    if (player instanceof ConnectedPlayer connectedPlayer && connectedPlayer.checkVersionCompatibility(targetBackend)) {
      return;
    }

    queue.enqueue(player);
    player.sendMessage(Component.translatable("velocity.queue.command.queued")
            .arguments(Argument.string("server", targetBackendName)));
  }

  /**
   * Handles the logic for when a player leaves.
   *
   * @param player The player that left.
   */
  @Override
  public final void onPlayerDisconnect(final Player player) {
    final long timeout = getTimeoutInSeconds(player);

    if (timeout == -1) {
      removePlayerEntirely(player);
    } else {
      this.server.getScheduler().buildTask(VelocityVirtualPlugin.INSTANCE, () ->
              removePlayerEntirely(player)).delay(timeout, TimeUnit.SECONDS).schedule();
    }
  }

  @Override
  public void removePlayerEntirely(final Player player) {
    for (Queue queue : this.getQueueCache().getQueues()) {
      if (queue.contains(player)) {
        queue.dequeue(player, false);
      }
    }
  }

  private void rescheduleTasks() {
    // Only schedule tasks on the master proxy
    if (!this.isMasterProxy()) {
      return;
    }

    this.scheduleTransferTask();
    this.scheduleBackendHandshakeTask();
    this.scheduleActionBarTask();
  }

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

  private void scheduleActionBarTask() {
    if (this.actionBarTask != null) {
      this.actionBarTask.cancel();
    }

    final VelocityConfiguration.Queue config = this.server.getConfiguration().getQueue();
    this.actionBarTask = this.server.getScheduler()
            .buildTask(VelocityVirtualPlugin.INSTANCE, () -> {
              for (Queue queue : this.getQueueCache().getQueues()) {
                this.broadcastActionBar(player -> ((AbstractQueue) queue).createActionbarComponent(player));
              }
            })
            .delay((long) (config.getMessageDelay() * 1000), TimeUnit.MILLISECONDS)
            .repeat((long) (config.getMessageDelay() * 1000), TimeUnit.MILLISECONDS)
            .schedule();
  }

  private void transfer() {
    if (!this.isMasterProxy()) {
      return;
    }

    // Process the first 10 queues to avoid overwhelming the system
    this.getQueueCache().getQueues().stream()
            .filter(queue -> queue.getState() == QueueState.ACTIVE)
            .filter(queue -> queue.getStatus() == ServerStatus.ONLINE)
            .filter(queue -> queue.size() > 0)
            .limit(10)
            .forEach(queue -> {
              final QueuePlayer queuePlayer = queue.getQueuePlayers().stream().findFirst().orElse(null);
              if (queuePlayer == null || queue.isFull() && !queuePlayer.isFullBypass()) {
                return;
              }

              // Transfer the player
              this.pollFirst(queue, queuePlayer);
            });
  }

  private void pingBackends() {
    // Process the first 5 servers to avoid overwhelming the system
    this.getQueueCache().getQueues().stream()
            .limit(5)
            .forEach(queue -> {
              RegisteredServer s = this.server.getServer(queue.getName()).orElse(null);
              if (s == null) {
                return;
              }

              // Use async ping with timeout
              s.ping().orTimeout(3, TimeUnit.SECONDS).whenComplete((result, th) -> {
                if (th != null) {
                  queue.setStatus(ServerStatus.OFFLINE);
                }

                if (queue.getStatus() == ServerStatus.OFFLINE && th == null) {
                  queue.setStatus(ServerStatus.WAITING);
                  LAST_TURNED_ONLINE_TIME.put(queue.getName(), System.currentTimeMillis());
                }

                if (th == null && queue.getQueuePlayers().stream().anyMatch(QueuePlayer::isQueueBypass)) {
                  queue.setStatus(ServerStatus.ONLINE);
                }

                final Long lastOnlineTime = LAST_TURNED_ONLINE_TIME.get(queue.getName());

                if (th == null && lastOnlineTime != null && queue.getStatus() == ServerStatus.WAITING) {
                  double queueDelay = this.server.getConfiguration().getQueue().getQueueDelay() * 1000;
                  if (System.currentTimeMillis() >= lastOnlineTime + queueDelay) {
                    queue.setStatus(ServerStatus.ONLINE);
                  }
                }

                if (queue.getStatus() != ServerStatus.ONLINE && queue.isOnline()) {
                  for (QueuePlayer queuePlayer : queue.getQueuePlayers()) {
                    if (queuePlayer.isQueueBypass()) {
                      queuePlayer.transfer();
                      queue.dequeue(queuePlayer.getPlayer(), false);
                    }
                  }
                }
              }).exceptionally(throwable -> {
                queue.setStatus(ServerStatus.OFFLINE);
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
