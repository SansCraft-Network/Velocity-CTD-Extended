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

package com.velocitypowered.proxy.redis;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.queue.redis.depot.QueueDepotService;
import com.velocitypowered.proxy.redis.impl.RouteRegistry;
import com.velocitypowered.proxy.redis.impl.TransactionHandlerRegistry;
import com.velocitypowered.proxy.redis.impl.depot.PlayerDepotService;
import com.velocitypowered.proxy.redis.impl.depot.ProxyDepotService;
import com.velocitypowered.proxy.redis.provider.LettuceProvider;
import com.velocitypowered.proxy.redis.provider.RedisProvider;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the main class of the Redis module within CTD
 *
 * @author Elmar Blume - 13/05/2025
 */
public final class VelocityRedis {

  public static @MonotonicNonNull VelocityRedis INSTANCE;

  private final VelocityServer server;
  private final RedisProvider provider;

  private final PlayerDepotService playerService;
  private final ProxyDepotService proxyService;
  private final QueueDepotService queueService;

  private final String proxyId;

  private boolean shutdown = false;

  public VelocityRedis(@NotNull VelocityServer server) {
    Preconditions.checkState(INSTANCE == null, "VelocityRedis is already initialized");
    INSTANCE = this;

    final VelocityConfiguration.Redis config = server.getConfiguration().getRedis();
    this.proxyId = config.getProxyId();

    this.server = server;
    this.provider = new LettuceProvider(config);
    this.provider.restart();

    this.playerService = new PlayerDepotService(this);
    this.proxyService = new ProxyDepotService(this);
    this.queueService = new QueueDepotService(this);

    this.registerRoutes();
    this.registerTransactionHandlers();
  }

  public void shutdown() {
    if (shutdown) {
      return;
    }
    shutdown = true;

    this.playerService.teardown();
    this.proxyService.teardown();
    this.queueService.teardown();
    this.provider.disconnect();
  }

  private void registerRoutes() {
    for (RouteRegistry registry : RouteRegistry.values()) {
      this.provider.registerRoute(registry.getRouteRegistration());
    }
  }

  private void registerTransactionHandlers() {
    for (TransactionHandlerRegistry registry : TransactionHandlerRegistry.values()) {
      this.provider.registerTransaction(registry.getTransactionHandler());
    }
  }

  public VelocityServer getServer() {
    if (server == null) {
      throw new IllegalStateException("You're trying to access the server of redis before it was initialized!");
    }

    return server;
  }

  public RedisProvider getProvider() {
    return provider;
  }

  public PlayerDepotService getPlayerService() {
    return playerService;
  }

  public ProxyDepotService getProxyService() {
    return proxyService;
  }

  public QueueDepotService getQueueService() {
    return queueService;
  }

  public String getProxyId() {
    return proxyId;
  }

  public boolean isShutdown() {
    return shutdown;
  }
}
