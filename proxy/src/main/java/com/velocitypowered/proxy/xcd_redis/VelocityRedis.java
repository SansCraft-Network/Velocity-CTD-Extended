package com.velocitypowered.proxy.xcd_redis;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.xcd_redis.impl.RouteRegistry;
import com.velocitypowered.proxy.xcd_redis.impl.TransactionRegistry;
import com.velocitypowered.proxy.xcd_redis.impl.depot.PlayerDepotService;
import com.velocitypowered.proxy.xcd_redis.impl.depot.ProxyDepotService;
import com.velocitypowered.proxy.xcd_redis.provider.LettuceProvider;
import com.velocitypowered.proxy.xcd_redis.provider.RedisProvider;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

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

  private final String proxyId;

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

    this.registerRoutes();
    this.registerTransactions();

  }

  private void registerRoutes() {
    for (RouteRegistry registry : RouteRegistry.values()) {
      this.provider.registerRoute(registry.getRouteRegistration());
    }
  }

  private void registerTransactions() {
    for (TransactionRegistry registry : TransactionRegistry.values()) {
      this.provider.registerTransaction(registry.getTransactionHandler());
    }
  }

  public VelocityServer getServer() {
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

  public String getProxyId() {
    return proxyId;
  }
}
