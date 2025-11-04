package com.velocitypowered.proxy.redis.impl.depot;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.redis.depot.DepotEntry;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elmar Blume - 18/05/2025
 */
public final class ProxyEntry extends DepotEntry<String, ProxyEntry> {

  public ProxyEntry(@NotNull VelocityServer server) {
    super(server.getProxyId());
  }
}
