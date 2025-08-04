package com.velocitypowered.proxy.xcd_redis.impl.depot;

import com.velocitypowered.proxy.xcd_redis.VelocityRedis;
import com.velocitypowered.proxy.xcd_redis.depot.AbstractDepotService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents an extension of the {@link AbstractDepotService} for the proxy depot, including
 * functionality to track certain information about a single proxy, or multiple proxies.
 *
 * @author Elmar Blume - 18/05/2025
 */
public final class ProxyDepotService extends AbstractDepotService<String, ProxyEntry> {

  private final VelocityRedis redis;

  /**
   * Constructs a new {@link ProxyDepotService}
   *
   * @param redis the {@link VelocityRedis} instance
   */
  public ProxyDepotService(@NotNull VelocityRedis redis) {
    super(ProxyEntry.class, redis.getProvider());
    this.redis = redis;
  }

  /**
   * Get a list of all the {@link ProxyEntry proxy} IDs currently present in the depot
   *
   * @return the list of all proxy IDs, sorted alphabetically
   */
  public List<String> getAllProxyIds() {
    return this.depot.keys().stream().sorted().toList();
  }
}
