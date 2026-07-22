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

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_14)) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_5)) {
        ProtocolUtils.writeVarInt(buf, 0); // 0 heightmaps
      } else {
        ProtocolUtils.writeBinaryTag(buf, version, net.kyori.adventure.nbt.CompoundBinaryTag.empty());
      }
    }

    boolean is118OrNewer = version.noLessThan(ProtocolVersion.MINECRAFT_1_18);
    int sectionCount = is118OrNewer ? 24 : 16;
    int stoneSectionIndex = is118OrNewer ? 4 : 0;

    ByteBuf sections = Unpooled.buffer(sectionCount * 16);
    for (int section = 0; section < sectionCount; section++) {
      if (section == stoneSectionIndex && chunkX == 0 && chunkZ == 0) {
        sections.writeShort(4096); // 4096 non-air blocks
        if (version.noLessThan(ProtocolVersion.MINECRAFT_26_1)) {
          sections.writeShort(4096); // fluid count short for 26.1+
        }
        sections.writeByte(0); // single-value palette
        ProtocolUtils.writeVarInt(sections, 1); // 1 = minecraft:stone
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_18)) {
          sections.writeByte(0); // single biome palette
          ProtocolUtils.writeVarInt(sections, 0); // biome 0
        }
      } else {
        sections.writeShort(0); // non-air blocks
        if (version.noLessThan(ProtocolVersion.MINECRAFT_26_1)) {
          sections.writeShort(0); // fluid count short for 26.1+
        }
        sections.writeByte(0); // single-value palette
        ProtocolUtils.writeVarInt(sections, 0); // air
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_18)) {
          sections.writeByte(0); // single biome palette
          ProtocolUtils.writeVarInt(sections, 0); // biome 0
        }
      }
    }
    ProtocolUtils.writeVarInt(buf, sections.readableBytes());
    buf.writeBytes(sections);
    sections.release();

    ProtocolUtils.writeVarInt(buf, 0); // block entities

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_18)) {
      if (version.lessThan(ProtocolVersion.MINECRAFT_1_20)) {
        buf.writeBoolean(true); // Trust edges
      }
      ProtocolUtils.writeVarInt(buf, 1);
      buf.writeLong(0L); // sky light mask (0 sections with arrays)
      ProtocolUtils.writeVarInt(buf, 1);
      buf.writeLong(0L); // block light mask (0 sections with arrays)
      ProtocolUtils.writeVarInt(buf, 0); // empty sky light mask
      ProtocolUtils.writeVarInt(buf, 0); // empty block light mask
      ProtocolUtils.writeVarInt(buf, 0); // sky light array count
      ProtocolUtils.writeVarInt(buf, 0); // block light array count
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_14)) {
      ProtocolUtils.writeVarInt(buf, 0); // sky light mask
      ProtocolUtils.writeVarInt(buf, 0); // block light mask
      ProtocolUtils.writeVarInt(buf, 0); // empty sky light mask
      ProtocolUtils.writeVarInt(buf, 0); // empty block light mask
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }
}