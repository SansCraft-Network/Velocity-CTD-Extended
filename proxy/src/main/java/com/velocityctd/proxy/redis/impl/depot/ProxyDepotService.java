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

package com.velocityctd.proxy.redis.impl.depot;

import com.velocityctd.proxy.redis.VelocityRedis;
import com.velocityctd.proxy.redis.depot.AbstractDepotService;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an extension of the {@link AbstractDepotService} for the proxy depot, including
 * functionality to track certain information about a single proxy, or multiple proxies.
 */
public final class ProxyDepotService extends AbstractDepotService<String, ProxyEntry> {

  /**
   * The Redis manager used to interact with proxy-related data stored in Redis.
   */
  private final VelocityRedis redis;

  /**
   * Constructs a new {@link ProxyDepotService}.
   *
   * @param redis the {@link VelocityRedis} instance
   */
  public ProxyDepotService(final @NotNull VelocityRedis redis) {
    super(ProxyEntry.class, redis.getProvider());

    this.redis = redis;

    this.depot.upsert(new ProxyEntry(redis.getServer()));
  }

  @Override
  public void teardown() {
    final ProxyEntry proxyEntry = this.get(this.redis.getServer().getProxyId());
    if (proxyEntry != null) {
      proxyEntry.remove();
    }
  }

  /**
   * Get a list of all the {@link ProxyEntry proxy} IDs currently present in the depot.
   *
   * @return the list of all proxy IDs, sorted alphabetically
   */
  public List<String> getAllProxyIds() {
    return this.depot.keys().stream().sorted().toList();
  }

  /**
   * Get a list of all the {@link ProxyEntry proxy} IDs currently present in the depot, in lower case.
   *
   * @return the list of all proxy IDs in lower case, sorted alphabetically
   */
  public List<String> getAllProxyIdsLowerCase() {
    return this.depot.keys().stream().map(String::toLowerCase).sorted().toList();
  }
}
