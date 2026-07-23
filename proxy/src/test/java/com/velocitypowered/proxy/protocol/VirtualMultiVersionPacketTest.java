/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.velocitypowered.proxy.protocol;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocityctd.api.server.VirtualGameMode;
import com.velocityctd.api.server.VirtualServerDefinition;
import com.velocityctd.api.server.VirtualServerHandler;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.packet.JoinGamePacket;
import com.velocitypowered.proxy.protocol.packet.virtual.VirtualChunkPacket;
import com.velocitypowered.proxy.protocol.packet.virtual.VirtualDefaultSpawnPacket;
import com.velocitypowered.proxy.protocol.packet.virtual.VirtualPlayerPositionPacket;
import com.velocitypowered.proxy.server.virtual.engine.VirtualProtocolEngine;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class VirtualMultiVersionPacketTest {

  private static final VirtualServerDefinition TEST_DEF = VirtualServerDefinition.builder("test-virtual")
      .gameMode(VirtualGameMode.SURVIVAL)
      .spawn(0.5, 16.0, 0.5, 90.0f, 0.0f)
      .worldTime(1000L)
      .handler(new VirtualServerHandler() {})
      .build();

  @Test
  void testJoinGamePacketEncode() {
    JoinGamePacket join = VirtualProtocolEngine.createJoinGamePacket(ProtocolVersion.MINECRAFT_26_2, TEST_DEF, true);
    ByteBuf buf = Unpooled.buffer();
    assertDoesNotThrow(() -> join.encode(buf, ProtocolUtils.Direction.CLIENTBOUND, ProtocolVersion.MINECRAFT_26_2));
    assertTrue(buf.readableBytes() > 0, "JoinGamePacket buffer should contain encoded data");
    buf.release();
  }

  @Test
  void testVirtualChunkPacketEncode() {
    VirtualChunkPacket chunk = new VirtualChunkPacket(0, 0);
    ByteBuf buf = Unpooled.buffer();
    assertDoesNotThrow(() -> chunk.encode(buf, ProtocolUtils.Direction.CLIENTBOUND, ProtocolVersion.MINECRAFT_26_2));
    assertTrue(buf.readableBytes() > 0, "VirtualChunkPacket buffer should contain encoded data");
    buf.release();
  }

  @Test
  void testVirtualPlayerPositionPacketEncode() {
    VirtualPlayerPositionPacket pos = new VirtualPlayerPositionPacket(0.5, 16.0, 0.5, 90.0f, 0.0f, 1);
    ByteBuf buf = Unpooled.buffer();
    assertDoesNotThrow(() -> pos.encode(buf, ProtocolUtils.Direction.CLIENTBOUND, ProtocolVersion.MINECRAFT_26_2));
    assertTrue(buf.readableBytes() > 0, "VirtualPlayerPositionPacket buffer should contain encoded data");
    buf.release();
  }

  @Test
  void testVirtualDefaultSpawnPacketEncode() {
    VirtualDefaultSpawnPacket spawn = new VirtualDefaultSpawnPacket("minecraft:overworld", 0, 16, 0, 90.0f);
    ByteBuf buf = Unpooled.buffer();
    assertDoesNotThrow(() -> spawn.encode(buf, ProtocolUtils.Direction.CLIENTBOUND, ProtocolVersion.MINECRAFT_26_2));
    assertTrue(buf.readableBytes() > 0, "VirtualDefaultSpawnPacket buffer should contain encoded data");
    buf.release();
  }
}
