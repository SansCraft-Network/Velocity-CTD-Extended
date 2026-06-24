/*
 * Copyright (C) 2026 Velocity-CTD Contributors
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

package com.velocityctd.proxy.server;

import com.velocityctd.api.server.VirtualServer;
import com.velocityctd.api.server.VirtualServerHandler;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityVirtualServerConnection extends VelocityServerConnection {

  private final VirtualServer virtualServer;
  private @Nullable VelocityVirtualConnection virtualConnection;

  public VelocityVirtualServerConnection(VelocityRegisteredServer registeredServer,
                                         @Nullable VelocityRegisteredServer previousServer,
                                         ConnectedPlayer proxyPlayer, VelocityServer server,
                                         VirtualServer virtualServer) {
    super(registeredServer, previousServer, proxyPlayer, server);
    this.virtualServer = virtualServer;
  }

  @Override
  public CompletableFuture<ConnectionRequestResults.Impl> connect() {
    CompletableFuture<ConnectionRequestResults.Impl> result = new CompletableFuture<>();
    getPlayer().getConnection().eventLoop().execute(() -> {
      try {
        VelocityServerConnection existing = getPlayer().getConnectedServer();
        if (existing != null) {
          existing.disconnect();
        }

        VirtualServerHandler handler = virtualServer.getHandler();
        this.virtualConnection = new VelocityVirtualConnection(getPlayer(), this);
        handler.onConnect(getPlayer(), this.virtualConnection);

        completeJoin();
        getPlayer().setConnectedServer(this);
        result.complete(ConnectionRequestResults.plainResult(ConnectionRequestBuilder.Status.SUCCESS, getServer()));
      } catch (Throwable t) {
        result.completeExceptionally(t);
      }
    });
    return result;
  }

  @Override
  public void disconnect() {
    getPlayer().getConnection().eventLoop().execute(() -> {
      virtualServer.getHandler().onDisconnect(getPlayer());
    });
  }
}
