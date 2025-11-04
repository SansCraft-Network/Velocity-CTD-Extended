package com.velocitypowered.proxy.redis.impl.depot;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.proxy.redis.depot.DepotEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * @author Elmar Blume - 18/05/2025
 */
public final class PlayerEntry extends DepotEntry<UUID, PlayerEntry> {

  private final String username;
  private final String proxyId;

  private String serverName = null;

  public PlayerEntry(UUID uniqueId, String username, String proxyId) {
    super(uniqueId);

    this.username = username;
    this.proxyId = proxyId;
  }

  public PlayerEntry(final @NotNull Player player, String proxyId) {
    this(player.getUniqueId(), player.getUsername(), proxyId);
    this.setServer(player.getCurrentServer().orElse(null));
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

  public String getServerName() {
    return serverName;
  }

  public void setServerName(String serverName) {
    this.serverName = serverName;
  }
}
