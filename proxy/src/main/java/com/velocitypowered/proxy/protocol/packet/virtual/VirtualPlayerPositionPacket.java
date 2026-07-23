/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.velocitypowered.proxy.protocol.packet.virtual;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public final class VirtualPlayerPositionPacket implements MinecraftPacket {
  private final double posX;
  private final double posY;
  private final double posZ;
  private final float yaw;
  private final float pitch;
  private final int teleportId;

  public VirtualPlayerPositionPacket(double posX, double posY, double posZ,
      float yaw, float pitch, int teleportId) {
    this.posX = posX;
    this.posY = posY;
    this.posZ = posZ;
    this.yaw = yaw;
    this.pitch = pitch;
    this.teleportId = teleportId;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_9)) {
      ProtocolUtils.writeVarInt(buf, teleportId);
      buf.writeDouble(posX).writeDouble(posY).writeDouble(posZ);
      buf.writeDouble(0).writeDouble(0).writeDouble(0);
      buf.writeFloat(yaw).writeFloat(pitch).writeInt(0);
    } else {
      buf.writeDouble(posX).writeDouble(posY).writeDouble(posZ);
      buf.writeFloat(yaw).writeFloat(pitch).writeByte(0);
      ProtocolUtils.writeVarInt(buf, teleportId);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }
}