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

import static com.velocitypowered.api.queue.QueueState.ACTIVE;
import static com.velocitypowered.api.queue.QueueState.PAUSED;
import static com.velocitypowered.api.queue.ServerStatus.FULL;
import static com.velocitypowered.api.queue.ServerStatus.OFFLINE;
import static com.velocitypowered.api.queue.ServerStatus.ONLINE;
import static com.velocitypowered.api.queue.ServerStatus.WAITING;
import static com.velocitypowered.proxy.util.PermissionUtils.findHighestPermissionValue;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.queue.Queue;
import com.velocitypowered.api.queue.QueueEntry;
import com.velocitypowered.api.queue.QueueManager;
import com.velocitypowered.api.queue.QueueState;
import com.velocitypowered.api.queue.ServerStatus;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.queue.redis.depot.VelocityQueueDepotEntry;
import com.velocitypowered.proxy.queue.redis.depot.VelocityQueueDepotService;
import com.velocitypowered.proxy.queue.redis.packet.VelocityQueueSync;
import com.velocitypowered.proxy.redis.impl.packet.VelocityActionBar;
import com.velocitypowered.proxy.redis.impl.packet.VelocityMessage;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Single implementation of {@link QueueManager}, handling both memory-only and Redis modes.
 */
public final class VelocityQueueManager implements QueueManager {

  /**
   * Timestamp (ms) of when each server last transitioned from OFFLINE to WAITING.
   * Shared across all calls to pingBackends on this proxy.
   */
  private static final Map<String, Long> LAST_TURNED_ONLINE_TIME = new ConcurrentHashMap<>();

  private final VelocityServer server;

  /**
   * Local in-memory queues keyed by server name.
   */
  private final ConcurrentHashMap<String, VelocityQueue> queues = new ConcurrentHashMap<>();

  private @Nullable ScheduledTask transferTask;
  private @Nullable ScheduledTask actionBarTask;
  private @Nullable ScheduledTask backendHandshakeTask;

  /**
   * Monotonically increasing tick counter used for the round-robin action-bar display.
   */
  private int actionBarTick = 0;

  /**
   * Constructs a new {@link VelocityQueueManager}, loading persisted state from Redis
   * (if enabled) and scheduling periodic tasks.
   *
   * @param server the proxy instance
   */
  public VelocityQueueManager(final @NotNull VelocityServer server) {
    this.server = server;

    // Initialise queues for every registered server
    if (server.isRedisEnabled()) {
      loadFromRedis();
    }

    // Ensure every registered server has a queue (fill gaps left by cold-start)
    for (RegisteredServer rs : server.getAllServers()) {
      final String name = rs.getServerInfo().getName();
      queues.computeIfAbsent(name, n -> {
        final boolean inactive = server.getConfiguration().getQueue()
            .getNoQueueServers().contains(n);
        return new VelocityQueue(server, this, (VelocityRegisteredServer) rs,
            inactive ? QueueState.INACTIVE : QueueState.ACTIVE);
      });
    }

    rescheduleTasks();

    // On Redis reconnect, reload queue state from the depot so any packets that were
    // dropped during the disconnection are recovered. Schedule on the Velocity scheduler
    // to avoid blocking Lettuce's internal I/O thread.
    if (server.isRedisEnabled()) {
      server.getRedis().addReconnectListener(() ->
          server.getScheduler()
              .buildTask(VelocityVirtualPlugin.INSTANCE, this::reloadFromRedis)
              .schedule());
    }
  }

  @Override
  public void reload() {
    rescheduleTasks();
  }

  @Override
  public void teardown() {
    cancelTasks();
    queues.values().forEach(VelocityQueue::teardown);
  }

  @Override
  public boolean isMasterProxy() {
    if (!server.isRedisEnabled()) {
      return true;
    }

    final List<String> masterProxies = server.getConfiguration().getQueue().getMasterProxyIds();
    final List<String> activeProxies = new ArrayList<>(
        server.getRedis().getProxyService().getAllProxyIds());
    Collections.sort(activeProxies);

    final String ownId = server.getProxyId();

    if (masterProxies.isEmpty() || (masterProxies.size() == 1 && masterProxies.getFirst().isEmpty())) {
      // No explicit master list: alphabetically-first active proxy is master
      return !activeProxies.isEmpty() && activeProxies.getFirst().equalsIgnoreCase(ownId);
    }

    // Find the first master-proxy ID that is currently active
    activeProxies.retainAll(masterProxies);
    if (activeProxies.isEmpty()) {
      return false;
    }

    for (String candidate : masterProxies) {
      if (activeProxies.contains(candidate)) {
        return candidate.equalsIgnoreCase(ownId);
      }
    }

    return false;
  }

  @Override
  public @NotNull Queue getQueue(final @NotNull String serverName) {
    final VelocityRegisteredServer rs = (VelocityRegisteredServer) server.getServer(serverName)
        .orElseThrow(() -> new IllegalArgumentException("Unknown server: " + serverName));

    return queues.computeIfAbsent(serverName, n -> {
      final boolean inactive = server.getConfiguration().getQueue().getNoQueueServers().contains(n);
      return new VelocityQueue(server, this, rs,
          inactive ? QueueState.INACTIVE : QueueState.ACTIVE);
    });
  }

  @Override
  public @NotNull Collection<Queue> getQueues() {
    return Collections.unmodifiableCollection(queues.values());
  }

  @Override
  public boolean isQueued(final @NotNull Player player) {
    return getQueueFor(player) != null;
  }

  @Override
  public @Nullable Queue getQueueFor(final @NotNull Player player) {
    for (VelocityQueue q : queues.values()) {
      if (q.contains(player.getUniqueId())) {
        return q;
      }
    }
    return null;
  }

  @Override
  public void queue(final @NotNull Player player, final @NotNull RegisteredServer targetServer) {
    final String targetName = targetServer.getServerInfo().getName();
    final VelocityQueue queue = (VelocityQueue) getQueue(targetName);

    final VelocityConfiguration.Queue config = server.getConfiguration().getQueue();
    if (!config.isEnabled() || player.hasPermission("velocity.queue.bypass")) {
      player.createConnectionRequest(targetServer).connectWithIndication();
      return;
    }

    if (queue.contains(player)) {
      player.sendMessage(Component.translatable("velocity.queue.error.already-queued")
          .arguments(Component.text(targetName)));
      return;
    }

    if (!config.isAllowMultiQueue()) {
      for (VelocityQueue q : queues.values()) {
        if (q.contains(player)) {
          q.dequeue(player);
          player.sendMessage(Component.translatable("velocity.queue.error.queued-swap")
              .arguments(
                  Argument.string("from", q.getName()),
                  Argument.string("to", targetName)));
          break;
        }
      }
    }

    if (queue.getState() == PAUSED && !config.isAllowPausedQueueJoining()) {
      player.sendMessage(Component.translatable("velocity.queue.error.paused")
          .arguments(Component.text(targetName)));
      return;
    }

    if (player instanceof ConnectedPlayer cp && cp.checkVersionCompatibility(targetServer)) {
      return;
    }

    queue.enqueue(player);
    player.sendMessage(Component.translatable("velocity.queue.command.queued")
        .arguments(Component.text(targetName)));
  }

  @Override
  public void onPlayerDisconnect(final @NotNull Player player) {
    if (server.isShuttingDown()) {
      removePlayerEntirely(player);
      return;
    }

    final int timeout = getTimeoutInSeconds(player);
    if (timeout == -1) {
      removePlayerEntirely(player);
    } else {
      server.getScheduler()
          .buildTask(VelocityVirtualPlugin.INSTANCE, () -> removePlayerEntirely(player))
          .delay(timeout, TimeUnit.SECONDS)
          .schedule();
    }
  }

  @Override
  public void removePlayerEntirely(final @NotNull Player player) {
    for (VelocityQueue queue : queues.values()) {
      if (queue.contains(player)) {
        queue.dequeue(player);
      }
    }
  }

  @Override
  public void broadcastMessage(final @NotNull Queue queue,
                               final @NotNull Function<QueueEntry, Component> componentFn) {
    for (QueueEntry entry : queue.getEntries()) {
      final Component msg = componentFn.apply(entry);
      if (server.isRedisEnabled()) {
        new VelocityMessage(entry.getUniqueId(), msg).publish();
      } else {
        server.getPlayer(entry.getUniqueId()).ifPresent(p -> p.sendMessage(msg));
      }
    }
  }

  /**
   * Applies a {@link VelocityQueueSync} packet received from another proxy to the local
   * in-memory queue. This is the mechanism that keeps all proxies in sync.
   *
   * @param packet the incoming sync packet
   */
  public void handleSync(final @NotNull VelocityQueueSync packet) {
    final VelocityQueueSync.Payload p = packet.getPayload();
    if (p == null) {
      return;
    }

    final VelocityQueue queue;
    try {
      queue = (VelocityQueue) getQueue(p.serverName());
    } catch (IllegalArgumentException ignored) {
      return; // unknown server
    }

    switch (p.action()) {
      case ENQUEUE -> queue.applyEnqueue(p);
      case DEQUEUE -> queue.applyDequeue(p.playerUuid());
      case STATE_CHANGE -> queue.applyStateChange(p.newState());
      case STATUS_CHANGE -> queue.applyStatusChange(p.newStatus());
      case WAITING_CHANGE -> queue.applyWaitingChange(p);
      default -> throw new IllegalStateException("Unknown action " + p.action() + ".");
    }
  }

  private void rescheduleTasks() {
    cancelTasks();

    scheduleActionBarTask();
    scheduleTransferTask();
    scheduleBackendHandshakeTask();
  }

  private void scheduleTransferTask() {
    final VelocityConfiguration.Queue config = server.getConfiguration().getQueue();
    transferTask = server.getScheduler()
        .buildTask(VelocityVirtualPlugin.INSTANCE, this::runTransfer)
        .delay((long) (config.getSendDelay() * 1000), TimeUnit.MILLISECONDS)
        .repeat((long) (config.getSendDelay() * 1000), TimeUnit.MILLISECONDS)
        .schedule();
  }

  private void scheduleBackendHandshakeTask() {
    final VelocityConfiguration.Queue config = server.getConfiguration().getQueue();
    backendHandshakeTask = server.getScheduler()
        .buildTask(VelocityVirtualPlugin.INSTANCE, this::pingBackends)
        .delay((long) (config.getBackendPingInterval() * 1000), TimeUnit.MILLISECONDS)
        .repeat((long) (config.getBackendPingInterval() * 1000), TimeUnit.MILLISECONDS)
        .schedule();
  }

  private void scheduleActionBarTask() {
    final VelocityConfiguration.Queue config = server.getConfiguration().getQueue();
    actionBarTask = server.getScheduler()
        .buildTask(VelocityVirtualPlugin.INSTANCE, this::sendActionBars)
        .delay((long) (config.getMessageDelay() * 1000), TimeUnit.MILLISECONDS)
        .repeat((long) (config.getMessageDelay() * 1000), TimeUnit.MILLISECONDS)
        .schedule();
  }

  private void cancelTasks() {
    if (transferTask != null) {
      transferTask.cancel();
      transferTask = null;
    }
    if (actionBarTask != null) {
      actionBarTask.cancel();
      actionBarTask = null;
    }
    if (backendHandshakeTask != null) {
      backendHandshakeTask.cancel();
      backendHandshakeTask = null;
    }
  }

  private void runTransfer() {
    if (!isMasterProxy()) {
      return;
    }

    final Set<UUID> transferredThisTick = new HashSet<>();

    queues.values().stream()
        .filter(q -> q.getState() == ACTIVE)
        .filter(q -> q.getServerStatus().isActive())
        .filter(q -> q.size() > 0)
        .forEach(queue -> {
          final VelocityQueueEntry candidate = queue.getInternalEntries().stream()
              .filter(e -> !transferredThisTick.contains(e.getUniqueId()))
              .filter(e -> queue.getServerStatus() != FULL || e.isFullBypass())
              .filter(e -> !e.isWaitingForConnection())
              .findFirst()
              .orElse(null);

          if (candidate == null) {
            return;
          }

          // Check player is still online somewhere
          final boolean online = server.isRedisEnabled()
              ? server.getRedis().getPlayerService().isPlayerOnline(candidate.getUniqueId())
              : server.getPlayer(candidate.getUniqueId()).isPresent();

          if (online) {
            queue.transferEntry(candidate);
            transferredThisTick.add(candidate.getUniqueId());
          } else {
            queue.removeEntry(candidate);
          }
        });
  }

  private void pingBackends() {
    if (!isMasterProxy()) {
      return;
    }

    for (VelocityQueue queue : queues.values()) {
      final RegisteredServer rs = server.getServer(queue.getName()).orElse(null);
      if (rs == null) {
        continue;
      }

      rs.ping().orTimeout(3, TimeUnit.SECONDS).whenComplete((result, th) -> {
        if (th != null) {
          queue.setServerStatus(OFFLINE);
          return;
        }

        final boolean serverFull = result.getPlayers()
            .map(p -> p.getMax() > 0 && p.getOnline() >= p.getMax())
            .orElse(false);
        final ServerStatus activeStatus = serverFull ? FULL : ONLINE;

        if (queue.getServerStatus() == OFFLINE) {
          queue.setServerStatus(WAITING);
          LAST_TURNED_ONLINE_TIME.put(queue.getName(), System.currentTimeMillis());
        }

        if (queue.getServerStatus().isActive()) {
          queue.setServerStatus(activeStatus);
          return;
        }

        // Still WAITING – check warmup delay
        final Long lastOnline = LAST_TURNED_ONLINE_TIME.get(queue.getName());
        if (lastOnline != null) {
          final double queueDelay = server.getConfiguration().getQueue().getQueueDelay() * 1000;
          if (System.currentTimeMillis() >= lastOnline + queueDelay) {
            queue.setServerStatus(activeStatus);
          }
        }
      }).exceptionally(th -> {
        queue.setServerStatus(OFFLINE);
        return null;
      });
    }
  }

  private void sendActionBars() {
    if (!isMasterProxy()) {
      return;
    }

    // Collect all entries per player UUID (player may be in multiple queues)
    final Map<UUID, List<VelocityQueueEntry>> byPlayer = new HashMap<>();

    queues.values()
        .stream()
        .sorted(Comparator.comparing(VelocityQueue::getName))
        .forEach(queue -> {
          for (VelocityQueueEntry entry : queue.getInternalEntries()) {
            byPlayer.computeIfAbsent(entry.getUniqueId(), k -> new ArrayList<>()).add(entry);
          }
        });

    for (List<VelocityQueueEntry> entries : byPlayer.values()) {
      final int index = (actionBarTick / 2) % entries.size();
      final VelocityQueueEntry entry = entries.get(index);
      sendActionBar(entry);
    }

    actionBarTick++;
  }

  private void sendActionBar(final VelocityQueueEntry entry) {
    final Component component = entry.getQueue().createActionbarComponent(entry);

    if (server.isRedisEnabled()) {
      new VelocityActionBar(entry.getUniqueId(), component).publish();
    } else {
      server.getPlayer(entry.getUniqueId()).ifPresent(p -> p.sendActionBar(component));
    }
  }

  /**
   * Reloads queue state from the Redis depot after a pub/sub reconnection.
   *
   * <p>Any packets that were missed during the disconnection window are recovered by
   * re-reading the master-written depot snapshot. If this proxy is currently master it
   * additionally re-broadcasts all server statuses and queue states so non-master proxies
   * can also recover.</p>
   */
  private void reloadFromRedis() {
    loadFromRedis();

    if (isMasterProxy()) {
      for (VelocityQueue queue : queues.values()) {
        new VelocityQueueSync(VelocityQueueSync.Payload.statusChange(
            queue.getName(), queue.getServerStatus())).publish();
        new VelocityQueueSync(VelocityQueueSync.Payload.stateChange(
            queue.getName(), queue.getState())).publish();
      }
    }
  }

  private void loadFromRedis() {
    final VelocityQueueDepotService service = server.getRedis().getQueueService();
    for (VelocityQueueDepotEntry entry : service.getAll()) {
      final VelocityRegisteredServer rs = (VelocityRegisteredServer) server
          .getServer(entry.getUniqueId()).orElse(null);
      if (rs != null) {
        queues.put(entry.getUniqueId(), new VelocityQueue(server, this, rs, entry));

        // If the persisted status is WAITING, seed the warmup timer so pingBackends() can
        // promote it to ONLINE/FULL once the delay elapses. Without this the queue would
        // stay stuck in WAITING forever because LAST_TURNED_ONLINE_TIME would have no entry.
        if (entry.getServerStatus() == WAITING) {
          LAST_TURNED_ONLINE_TIME.put(entry.getUniqueId(), System.currentTimeMillis());
        }
      }
    }
  }

  private int getTimeoutInSeconds(final Player player) {
    if (player.hasPermission("velocity.queue.timeout.exempt")) {
      return 0;
    }

    return findHighestPermissionValue(player, "velocity.queue.timeout.", 86_400)
        .orElse(-1);
  }
}
