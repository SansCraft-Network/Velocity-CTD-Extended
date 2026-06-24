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

import com.velocityctd.api.server.VirtualConnection;
import com.velocityctd.api.server.VirtualPacketHandler;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.StateRegistry;
import net.kyori.adventure.text.Component;

public class VelocityVirtualConnection implements VirtualConnection {

  private final ConnectedPlayer player;
  private final VelocityVirtualServerConnection serverConnection;
  private final VelocityVirtualSessionHandler sessionHandler;

  public VelocityVirtualConnection(ConnectedPlayer player, VelocityVirtualServerConnection serverConnection) {
    this.player = player;
    this.serverConnection = serverConnection;
    this.sessionHandler = new VelocityVirtualSessionHandler(this);
    player.getConnection().setActiveSessionHandler(StateRegistry.PLAY, this.sessionHandler);
  }

  @Override
  public void sendPacket(Object packet) {
    player.getConnection().write(packet);
  }

  @Override
  public void disconnect(Component reason) {
    player.disconnect(reason);
  }

  @Override
  public ProtocolVersion getProtocolVersion() {
    return player.getProtocolVersion();
  }

  @Override
  public void setPacketHandler(VirtualPacketHandler handler) {
    this.sessionHandler.setPacketHandler(handler);
  }
}
