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

package com.velocitypowered.proxy.server;

import static java.util.Objects.requireNonNull;

import com.velocityctd.api.server.VirtualConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.PluginMessageEncoder;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;

/**
 * Logical association between a player and a proxy-managed virtual server.
 */
public final class VelocityVirtualConnection implements VirtualConnection,
    com.velocitypowered.api.proxy.ServerConnection {

  private final VelocityVirtualRegisteredServer server;
  private final ConnectedPlayer player;
  private final RegisteredServer previousServer;
  private final AtomicBoolean closed = new AtomicBoolean();

  public VelocityVirtualConnection(VelocityVirtualRegisteredServer server,
      ConnectedPlayer player, RegisteredServer previousServer) {
    this.server = requireNonNull(server, "server");
    this.player = requireNonNull(player, "player");
    this.previousServer = previousServer;
  }

  @Override
  public VelocityVirtualRegisteredServer getServer() {
    return server;
  }

  @Override
  public Optional<RegisteredServer> getPreviousServer() {
    return Optional.ofNullable(previousServer);
  }

  @Override
  public ServerInfo getServerInfo() {
    return server.getServerInfo();
  }

  @Override
  public ConnectedPlayer getPlayer() {
    return player;
  }

  @Override
  public boolean sendPluginMessage(@NotNull ChannelIdentifier identifier,
      byte @NotNull [] data) {
    return false;
  }

  @Override
  public boolean sendPluginMessage(@NotNull ChannelIdentifier identifier,
      @NotNull PluginMessageEncoder dataEncoder) {
    return false;
  }

  public boolean isClosed() {
    return closed.get();
  }

  /**
   * Ends this virtual association and emits its lifecycle callback exactly once.
   */
  public void close() {
    if (closed.compareAndSet(false, true)) {
      server.removePlayer(player);
      try {
        server.getDefinition().getHandler().onDisconnect(player, this);
      } catch (RuntimeException exception) {
        org.slf4j.LoggerFactory.getLogger(VelocityVirtualConnection.class)
            .error("Virtual server disconnect handler failed for {}", player, exception);
      }
    }
  }
}