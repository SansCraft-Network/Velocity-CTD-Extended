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
import io.netty.buffer.Unpooled;

/**
 * Encodes one full overworld chunk containing a solid stone platform at Y=0..15.
 */
public final class VirtualChunkPacket implements MinecraftPacket {
  private final int chunkX;
  private final int chunkZ;

  public VirtualChunkPacket(int chunkX, int chunkZ) {
    this.chunkX = chunkX;
    this.chunkZ = chunkZ;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    buf.writeInt(chunkX).writeInt(chunkZ);
    ProtocolUtils.writeVarInt(buf, 0); // no height maps

    ByteBuf sections = Unpooled.buffer(24 * 8);
    for (int section = 0; section < 24; section++) {
      if (section == 4 && chunkX == 0 && chunkZ == 0) {
        // Section 4 (Y=0..15): Solid Stone platform (blockStateId 1)
        sections.writeShort(4096); // 4096 non-air blocks
        sections.writeShort(0);    // 0 non-empty fluids
        sections.writeByte(0);     // single-valued palette
        ProtocolUtils.writeVarInt(sections, 1); // 1 = minecraft:stone
        sections.writeByte(0);     // single biome palette
        ProtocolUtils.writeVarInt(sections, 0); // biome 0
      } else {
        // Empty air section
        sections.writeShort(0); // non-air blocks
        sections.writeShort(0); // non-empty fluids
        sections.writeByte(0); // single block palette
        ProtocolUtils.writeVarInt(sections, 0); // air
        sections.writeByte(0); // single biome palette
        ProtocolUtils.writeVarInt(sections, 0); // first synchronized biome
      }
    }
    ProtocolUtils.writeVarInt(buf, sections.readableBytes());
    buf.writeBytes(sections);
    sections.release();

    ProtocolUtils.writeVarInt(buf, 0); // block entities
    ProtocolUtils.writeVarInt(buf, 0); // sky light mask
    ProtocolUtils.writeVarInt(buf, 0); // block light mask
    ProtocolUtils.writeVarInt(buf, 0); // empty sky light mask
    ProtocolUtils.writeVarInt(buf, 0); // empty block light mask
    ProtocolUtils.writeVarInt(buf, 0); // sky light arrays
    ProtocolUtils.writeVarInt(buf, 0); // block light arrays
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }
}