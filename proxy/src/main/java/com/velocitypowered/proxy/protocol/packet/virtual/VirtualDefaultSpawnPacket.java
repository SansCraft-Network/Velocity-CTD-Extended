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

public final class VirtualDefaultSpawnPacket implements MinecraftPacket {
  private final String dimension;
  private final int posX;
  private final int posY;
  private final int posZ;
  private final float yaw;

  public VirtualDefaultSpawnPacket(String dimension, int posX, int posY, int posZ, float yaw) {
    this.dimension = dimension;
    this.posX = posX;
    this.posY = posY;
    this.posZ = posZ;
    this.yaw = yaw;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    ProtocolUtils.writeString(buf, dimension);
    long position = ((posX & 0x3FFFFFFL) << 38)
        | ((posZ & 0x3FFFFFFL) << 12) | (posY & 0xFFFL);
    buf.writeLong(position);
    buf.writeFloat(yaw);
    buf.writeFloat(0);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }
}