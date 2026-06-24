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

import com.velocityctd.api.server.VirtualPacketHandler;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Underlying Netty session handler intercepting play packets and forwarding them to the
 * registered VirtualPacketHandler.
 */
public class VelocityVirtualSessionHandler implements MinecraftSessionHandler {

  private final VelocityVirtualConnection connection;
  private @Nullable VirtualPacketHandler packetHandler;

  public VelocityVirtualSessionHandler(VelocityVirtualConnection connection) {
    this.connection = connection;
  }

  public void setPacketHandler(VirtualPacketHandler packetHandler) {
    this.packetHandler = packetHandler;
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    VirtualPacketHandler handler = this.packetHandler;
    if (handler != null) {
      handler.handlePacket(packet);
    }
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    VirtualPacketHandler handler = this.packetHandler;
    if (handler != null) {
      handler.handlePacket(buf);
    }
  }
}
