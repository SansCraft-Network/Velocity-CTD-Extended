/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.netty;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.util.except.QuietRuntimeException;
import com.velocitypowered.proxy.util.ratelimit.PacketLimiter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.CorruptedFrameException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Decodes Minecraft packets.
 */
public class MinecraftDecoder extends ChannelInboundHandlerAdapter {

  public static final boolean DEBUG = Boolean.getBoolean("velocity.packet-decode-logging");
  private static final QuietRuntimeException DECODE_FAILED =
      new QuietRuntimeException("A packet did not decode successfully (invalid data). For more "
          + "information, launch Velocity with -Dvelocity.packet-decode-logging=true to see more.");
  private static final Logger logger = LogManager.getLogger(MinecraftDecoder.class);

  private final ProtocolUtils.Direction direction;
  @Nullable private final PacketLimiter packetLimiter;
  private StateRegistry state;
  private StateRegistry.PacketRegistry.ProtocolRegistry registry;

  /**
   * Creates a new {@code MinecraftDecoder} decoding packets from the specified {@code direction}.
   *
   * @param direction the direction from which we decode from
   */
  public MinecraftDecoder(final ProtocolUtils.Direction direction, @Nullable final PacketLimiter packetLimiter) {
    this.direction = Preconditions.checkNotNull(direction, "direction");
    this.packetLimiter = packetLimiter;
    this.registry = StateRegistry.HANDSHAKE.getProtocolRegistry(
        direction, ProtocolVersion.MINIMUM_VERSION);
    this.state = StateRegistry.HANDSHAKE;
  }

  @Override
  public void channelRead(@NotNull final ChannelHandlerContext ctx, @NotNull final Object msg) throws Exception {
    if (msg instanceof ByteBuf buf) {
      tryDecode(ctx, buf);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  private void tryDecode(final ChannelHandlerContext ctx, final ByteBuf buf) throws Exception {
    if (!ctx.channel().isActive() || !buf.isReadable()) {
      buf.release();
      return;
    }

    int originalReaderIndex = buf.readerIndex();
    int packetId = ProtocolUtils.readVarInt(buf);
    MinecraftPacket packet = this.registry.createPacket(packetId);
    if (packet == null) {
      buf.readerIndex(originalReaderIndex);
      ctx.fireChannelRead(buf);
    } else {
      try {
        doLengthSanityChecks(buf, packet);

        try {
          packet.decode(buf, direction, registry.version);
        } catch (Exception e) {
          throw handleDecodeFailure(e, packet, packetId);
        }

        if (buf.isReadable()) {
          throw handleOverflow(packet, buf.readerIndex(), buf.writerIndex());
        }
        ctx.fireChannelRead(packet);
      } finally {
        buf.release();
      }
    }
  }

  private void doLengthSanityChecks(final ByteBuf buf, final MinecraftPacket packet) throws Exception {
    int readableBytes = buf.readableBytes();

    if (packetLimiter != null && !packetLimiter.incrementAndCheck(readableBytes)) {
      throw new IllegalStateException("Packet rate limit exceeded (" + packetLimiter.getCounter()
          + " packets and " + packetLimiter.getDataCounter() + " bytes per second)");
    }

    int expectedMinLen = packet.expectedMinLength(buf, direction, registry.version);
    int expectedMaxLen = packet.expectedMaxLength(buf, direction, registry.version);
    if (expectedMaxLen != -1 && readableBytes > expectedMaxLen) {
      throw handleOverflow(packet, expectedMaxLen, readableBytes);
    }
    if (readableBytes < expectedMinLen) {
      throw handleUnderflow(packet, expectedMaxLen, readableBytes);
    }
  }

  private Exception handleOverflow(final MinecraftPacket packet, final int expected, final int actual) {
    if (DEBUG) {
      return new CorruptedFrameException("Packet sent for " + packet.getClass() + " was too "
          + "big (expected " + expected + " bytes, got " + actual + " bytes)");
    } else {
      return DECODE_FAILED;
    }
  }

  private Exception handleUnderflow(final MinecraftPacket packet, final int expected, final int actual) {
    if (DEBUG) {
      return new CorruptedFrameException("Packet sent for " + packet.getClass() + " was too "
          + "small (expected " + expected + " bytes, got " + actual + " bytes)");
    } else {
      return DECODE_FAILED;
    }
  }

  private Exception handleDecodeFailure(final Exception cause, final MinecraftPacket packet, final int packetId) {
    if (DEBUG) {
      return new CorruptedFrameException(
          "Error decoding " + packet.getClass() + " " + getExtraConnectionDetail(packetId), cause);
    } else {
      return DECODE_FAILED;
    }
  }

  private String getExtraConnectionDetail(final int packetId) {
    return "Direction " + direction + " Protocol " + registry.version + " State " + state
        + " ID " + Integer.toHexString(packetId);
  }

  public void setProtocolVersion(final ProtocolVersion protocolVersion) {
    this.registry = state.getProtocolRegistry(direction, protocolVersion);
  }

  public void setState(final StateRegistry state) {
    this.state = state;
    this.setProtocolVersion(registry.version);
  }

  public ProtocolUtils.Direction getDirection() {
    return direction;
  }

  public PacketLimiter getPacketLimiter() {
    return packetLimiter;
  }
}
