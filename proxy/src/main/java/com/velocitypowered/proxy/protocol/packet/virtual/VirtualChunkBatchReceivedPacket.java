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

public final class VirtualChunkBatchReceivedPacket implements MinecraftPacket {

  private float desiredChunksPerTick;

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (buf.readableBytes() >= 4) {
      desiredChunksPerTick = buf.readFloat();
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    buf.writeFloat(desiredChunksPerTick);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }
}
