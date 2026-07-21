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

import com.velocityctd.api.server.VirtualServer;
import com.velocityctd.api.server.VirtualServerDefinition;
import com.velocityctd.proxy.queue.VelocityQueue;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.PluginMessageEncoder;
import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * A registered server backed by the proxy rather than a backend socket.
 */
public final class VelocityVirtualRegisteredServer extends VelocityRegisteredServer
    implements VirtualServer {

  private static final InetSocketAddress VIRTUAL_ADDRESS =
      InetSocketAddress.createUnresolved("virtual.invalid", 0);

  private final VirtualServerDefinition definition;

  public VelocityVirtualRegisteredServer(VelocityServer server,
      VirtualServerDefinition definition) {
    super(server, new ServerInfo(definition.getName(), VIRTUAL_ADDRESS));
    this.definition = definition;
  }

  @Override
  public VirtualServerDefinition getDefinition() {
    return definition;
  }

  @Override
  public long getTotalPlayerCount() {
    return getPlayerCount();
  }

  @Override
  public CompletableFuture<ServerPing> ping() {
    return CompletableFuture.completedFuture(createPing());
  }

  @Override
  public CompletableFuture<ServerPing> ping(PingOptions pingOptions) {
    return CompletableFuture.completedFuture(createPing());
  }

  private ServerPing createPing() {
    return ServerPing.builder()
        .version(new ServerPing.Version(0, "Velocity-CTD Virtual"))
        .onlinePlayers((int) getPlayerCount())
        .maximumPlayers(Integer.MAX_VALUE)
        .description(definition.getDescription())
        .build();
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

  @Override
  public boolean sendPluginMessage(ChannelIdentifier identifier, ByteBuf data) {
    data.release();
    return false;
  }

  @Override
  public VelocityQueue<?> getQueue() {
    throw new UnsupportedOperationException("Virtual servers do not use backend queues");
  }

  @Override
  public String toString() {
    return "virtual server: " + getServerInfo().getName();
  }
}