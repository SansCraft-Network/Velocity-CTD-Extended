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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocityctd.api.server.VirtualServer;
import com.velocityctd.api.server.VirtualServerDefinition;
import com.velocitypowered.api.event.proxy.server.ServerRegisteredEvent;
import com.velocitypowered.api.event.proxy.server.ServerUnregisteredEvent;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.VelocityServer;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implements the Velocity server registry.
 */
public class ServerMap {

  private final @Nullable VelocityServer server;

  private final Map<String, VelocityRegisteredServer> servers = new ConcurrentHashMap<>();

  public ServerMap(@Nullable VelocityServer server) {
    this.server = server;
  }

  /**
   * Returns the server associated with the given name.
   *
   * @param name the name to look up
   * @return the server, if it exists
   */
  public Optional<VelocityRegisteredServer> getServer(String name) {
    Preconditions.checkNotNull(name, "server");
    String lowerName = name.toLowerCase(Locale.US);
    return Optional.ofNullable(servers.get(lowerName));
  }

  public Collection<VelocityRegisteredServer> getAllServers() {
    return ImmutableList.copyOf(servers.values());
  }

  /**
   * Creates a raw implementation of a {@link VelocityRegisteredServer} without tying it to the internal
   * server map.
   *
   * @param serverInfo the server to create a registered server with
   * @return the {@link VelocityRegisteredServer} built from the {@link ServerInfo}
   */
  public VelocityRegisteredServer createRawRegisteredServer(ServerInfo serverInfo) {
    return new VelocityRegisteredServer(server, serverInfo);
  }

  /**
   * Registers a server with the proxy.
   *
   * @param serverInfo the server to register
   * @return the registered server
   */
  public VelocityRegisteredServer register(ServerInfo serverInfo) {
    Preconditions.checkNotNull(serverInfo, "serverInfo");
    String lowerName = serverInfo.getName().toLowerCase(Locale.US);
    VelocityRegisteredServer rs = createRawRegisteredServer(serverInfo);

    VelocityRegisteredServer existing = servers.putIfAbsent(lowerName, rs);
    if (existing != null && !existing.getServerInfo().equals(serverInfo)) {
      throw new IllegalArgumentException(
          "Server with name " + serverInfo.getName() + " already registered");
    } else if (existing == null) {
      if (server != null) {
        server.getEventManager().fireAndForget(new ServerRegisteredEvent(rs));
      }

      return rs;
    } else {
      return existing;
    }
  }

  /**
   * Registers a virtual server with the proxy.
   *
   * @param definition the virtual server definition
   * @return the registered virtual server
   */
  public VelocityVirtualRegisteredServer registerVirtual(
      VirtualServerDefinition definition) {
    Preconditions.checkNotNull(definition, "definition");
    Preconditions.checkState(server != null,
        "A Velocity proxy instance is required to register a virtual server");
    String lowerName = definition.getName().toLowerCase(Locale.US);
    VelocityVirtualRegisteredServer virtualServer =
        new VelocityVirtualRegisteredServer(server, definition);
    VelocityRegisteredServer existing = servers.putIfAbsent(lowerName, virtualServer);
    if (existing != null) {
      throw new IllegalArgumentException(
          "Server with name " + definition.getName() + " already registered");
    }
    server.getEventManager().fireAndForget(new ServerRegisteredEvent(virtualServer));
    return virtualServer;
  }

  /**
   * Unregisters the specified server from the proxy.
   *
   * @param serverInfo the server to unregister
   */
  public void unregister(ServerInfo serverInfo) {
    Preconditions.checkNotNull(serverInfo, "serverInfo");
    String lowerName = serverInfo.getName().toLowerCase(Locale.US);
    VelocityRegisteredServer rs = servers.get(lowerName);
    if (rs == null) {
      throw new IllegalArgumentException(
          "Server with name " + serverInfo.getName() + " is not registered!");
    }

    Preconditions.checkArgument(rs.getServerInfo().equals(serverInfo),
        "Trying to remove server %s with differing information", serverInfo.getName());
    Preconditions.checkState(servers.remove(lowerName, rs),
        "Server with name %s replaced whilst unregistering", serverInfo.getName());

    if (server != null) {
      server.getEventManager().fireAndForget(new ServerUnregisteredEvent(rs));
    }
  }

  /**
   * Unregisters a virtual server from the proxy.
   *
   * @param virtualServer the virtual server
   */
  public void unregisterVirtual(VirtualServer virtualServer) {
    Preconditions.checkNotNull(virtualServer, "virtualServer");
    String name = virtualServer.getServerInfo().getName();
    String lowerName = name.toLowerCase(Locale.US);
    VelocityRegisteredServer registered = servers.get(lowerName);
    Preconditions.checkArgument(registered == virtualServer,
        "Virtual server with name %s is not registered", name);
    Preconditions.checkState(servers.remove(lowerName, registered),
        "Server with name %s replaced whilst unregistering", name);
    if (server != null) {
      server.getEventManager().fireAndForget(new ServerUnregisteredEvent(registered));
    }
  }
}
