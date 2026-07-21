/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.velocitypowered.proxy.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.packet.virtual.VirtualMovePacket;
import com.velocitypowered.proxy.protocol.packet.virtual.VirtualMovePositionPacket;
import com.velocitypowered.proxy.protocol.packet.virtual.VirtualMoveRotationPacket;
import com.velocitypowered.proxy.protocol.packet.virtual.VirtualMoveStatusPacket;
import com.velocitypowered.proxy.protocol.packet.virtual.VirtualTeleportConfirmPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class VirtualPacketRegistryTest {

  @ParameterizedTest
  @MethodSource("roundTrips")
  void virtualServerboundPacketsRoundTrip(int packetId, byte[] payload) {
    StateRegistry.PacketRegistry.ProtocolRegistry registry = StateRegistry.PLAY.serverbound
        .getProtocolRegistry(ProtocolVersion.MINECRAFT_26_2);
    MinecraftPacket packet = registry.createPacket(packetId);
    ByteBuf input = Unpooled.wrappedBuffer(payload);
    packet.decode(input, ProtocolUtils.Direction.SERVERBOUND,
        ProtocolVersion.MINECRAFT_26_2);

    ByteBuf output = Unpooled.buffer();
    packet.encode(output, ProtocolUtils.Direction.SERVERBOUND,
        ProtocolVersion.MINECRAFT_26_2);
    byte[] encoded = new byte[output.readableBytes()];
    output.readBytes(encoded);
    assertArrayEquals(payload, encoded);
  }

  @Test
  void usesVerified262PacketIds() {
    StateRegistry.PacketRegistry.ProtocolRegistry registry = StateRegistry.PLAY.serverbound
        .getProtocolRegistry(ProtocolVersion.MINECRAFT_26_2);
    assertEquals(0x00, registry.getPacketId(new VirtualTeleportConfirmPacket()));
    assertEquals(0x1E, registry.getPacketId(new VirtualMovePositionPacket()));
    assertEquals(0x1F, registry.getPacketId(new VirtualMovePacket()));
    assertEquals(0x20, registry.getPacketId(new VirtualMoveRotationPacket()));
    assertEquals(0x21, registry.getPacketId(new VirtualMoveStatusPacket()));
  }

  private static Stream<Arguments> roundTrips() {
    ByteBuf position = Unpooled.buffer().writeDouble(1).writeDouble(64)
        .writeDouble(-2).writeByte(3);
    ByteBuf positionRotation = Unpooled.buffer().writeDouble(1).writeDouble(64)
        .writeDouble(-2).writeFloat(90).writeFloat(10).writeByte(3);
    ByteBuf rotation = Unpooled.buffer().writeFloat(90).writeFloat(10).writeByte(3);
    return Stream.of(
        Arguments.of(0x00, new byte[]{42}),
        Arguments.of(0x1E, bytes(position)),
        Arguments.of(0x1F, bytes(positionRotation)),
        Arguments.of(0x20, bytes(rotation)),
        Arguments.of(0x21, new byte[]{3}));
  }

  private static byte[] bytes(ByteBuf buffer) {
    byte[] bytes = new byte[buffer.readableBytes()];
    buffer.readBytes(bytes);
    return bytes;
  }
}