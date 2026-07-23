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

package com.velocitypowered.proxy.protocol.packet.virtual;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.server.VelocityVirtualSessionHandler;
import io.netty.buffer.ByteBuf;

public final class VirtualMovePacket implements MinecraftPacket {

  private double posX;
  private double stance;
  private double posY;
  private double posZ;
  private float yaw;
  private float pitch;
  private boolean onGround;
  private int movementFlags;

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion version) {
    posX = buf.readDouble();
    if (version.lessThan(ProtocolVersion.MINECRAFT_1_8)) {
      stance = buf.readDouble();
    }
    posY = buf.readDouble();
    posZ = buf.readDouble();
    yaw = buf.readFloat();
    pitch = buf.readFloat();
    movementFlags = buf.readUnsignedByte();
    onGround = (movementFlags & 1) != 0;
    if (buf.readableBytes() > 0) {
      buf.skipBytes(buf.readableBytes());
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion version) {
    buf.writeDouble(posX);
    if (version.lessThan(ProtocolVersion.MINECRAFT_1_8)) {
      buf.writeDouble(stance);
    }
    buf.writeDouble(posY);
    buf.writeDouble(posZ);
    buf.writeFloat(yaw);
    buf.writeFloat(pitch);
    buf.writeByte(version.lessThan(ProtocolVersion.MINECRAFT_1_21_2)
        ? (onGround ? 1 : 0) : movementFlags);
  }

  @Override
  public int decodeExpectedMaxLength(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion version) {
    return -1;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler instanceof VelocityVirtualSessionHandler virtual
        && virtual.handle(this);
  }

  public double getX() {
    return posX;
  }

  public double getY() {
    return posY;
  }

  public double getZ() {
    return posZ;
  }

  public float getYaw() {
    return yaw;
  }

  public float getPitch() {
    return pitch;
  }

  public boolean isOnGround() {
    return onGround;
  }
}