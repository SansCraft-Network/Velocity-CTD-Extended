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

public final class VirtualTimePacket implements MinecraftPacket {
  private final long worldAge;
  private final long timeOfDay;

  public VirtualTimePacket(long worldAge, long timeOfDay) {
    this.worldAge = worldAge;
    this.timeOfDay = timeOfDay;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    buf.writeLong(worldAge);
    if (version.noLessThan(ProtocolVersion.MINECRAFT_26_1)) {
      ProtocolUtils.writeVarInt(buf, 1);
      ProtocolUtils.writeVarInt(buf, 0);
      buf.writeLong(timeOfDay).writeBoolean(false);
    } else {
      buf.writeLong(timeOfDay);
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
        buf.writeBoolean(false);
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }
}