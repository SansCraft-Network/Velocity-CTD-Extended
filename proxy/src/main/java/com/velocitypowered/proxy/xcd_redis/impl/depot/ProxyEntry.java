package com.velocitypowered.proxy.xcd_redis.impl.depot;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.xcd_redis.depot.DepotEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author Elmar Blume - 18/05/2025
 */
public final class ProxyEntry extends DepotEntry<String, ProxyEntry> {

  public ProxyEntry(@NotNull VelocityServer server) {
    super(server.getProxyId());
  }
}
