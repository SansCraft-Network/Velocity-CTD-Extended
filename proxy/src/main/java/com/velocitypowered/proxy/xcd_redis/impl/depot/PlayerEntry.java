package com.velocitypowered.proxy.xcd_redis.impl.depot;

import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.proxy.xcd_redis.depot.DepotEntry;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * @author Elmar Blume - 18/05/2025
 */
public final class PlayerEntry extends DepotEntry<UUID, PlayerEntry> {

  private final transient boolean manual;

  private final String username;
  private final String proxyId;

  private final Map<String, Integer> queuePriority;
  private final boolean fullQueueBypass;
  private final boolean queueBypass;

  private String serverName = null;

  public PlayerEntry(UUID uniqueId, String username, String proxyId, Map<String, Integer> queuePriority, boolean fullQueueBypass, boolean queueBypass) {
    super(uniqueId);
    this.manual = true;//todo check if this is needed

    this.username = username;
    this.proxyId = proxyId;
    this.queuePriority = queuePriority;
    this.fullQueueBypass = fullQueueBypass;
    this.queueBypass = queueBypass;
  }

  @Override
  public void upsert() {
    if (this.manual) {
      return;
    }

    super.upsert();
  }

  public void setServer(@Nullable ServerConnection connection) {
    if (connection == null) {
      this.serverName = null;
      return;
    }

    this.serverName = connection.getServerInfo().getName();
  }

  public String getUsername() {
    return username;
  }

  public String getProxyId() {
    return proxyId;
  }

  public Map<String, Integer> getQueuePriority() {
    return queuePriority;
  }

  public boolean isFullQueueBypass() {
    return fullQueueBypass;
  }

  public boolean isQueueBypass() {
    return queueBypass;
  }

  public String getServerName() {
    return serverName;
  }

  public void setServerName(String serverName) {
    this.serverName = serverName;
  }
}
