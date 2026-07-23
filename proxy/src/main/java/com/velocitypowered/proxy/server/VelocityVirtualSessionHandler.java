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

package com.velocitypowered.proxy.server;

import com.velocityctd.api.server.VirtualPosition;
import com.velocityctd.api.server.VirtualServerHandler;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.ClientSettingsPacket;
import com.velocitypowered.proxy.protocol.packet.KeepAlivePacket;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerCommandPacket;
import com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerCommandPacket;
import com.velocitypowered.proxy.protocol.packet.virtual.VirtualMovePacket;
import com.velocitypowered.proxy.protocol.packet.virtual.VirtualMovePositionPacket;
import com.velocitypowered.proxy.protocol.packet.virtual.VirtualMoveRotationPacket;
import com.velocitypowered.proxy.protocol.packet.virtual.VirtualTeleportConfirmPacket;
import com.velocitypowered.proxy.protocol.packet.virtual.VirtualChunkBatchReceivedPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles client traffic while a player is held by a proxy-managed virtual server.
 */
public final class VelocityVirtualSessionHandler implements MinecraftSessionHandler {
  private static final Logger LOGGER = LogManager.getLogger(VelocityVirtualSessionHandler.class);

  private final VelocityServer proxy;
  private final ConnectedPlayer player;
  private final VelocityVirtualConnection connection;
  private double posX;
  private double posY;
  private double posZ;
  private float yaw;
  private float pitch;
  private ScheduledFuture<?> keepAliveTask;

  public VelocityVirtualSessionHandler(VelocityServer proxy, ConnectedPlayer player,
      VelocityVirtualConnection connection) {
    this.proxy = proxy;
    this.player = player;
    this.connection = connection;
    this.posX = connection.getServer().getDefinition().getSpawnX();
    this.posY = connection.getServer().getDefinition().getSpawnY();
    this.posZ = connection.getServer().getDefinition().getSpawnZ();
    this.yaw = connection.getServer().getDefinition().getSpawnYaw();
    this.pitch = connection.getServer().getDefinition().getSpawnPitch();
  }

  private VirtualServerHandler handler() {
    return connection.getServer().getDefinition().getHandler();
  }

  @Override
  public void activated() {
    LOGGER.info("[VirtualServer-Debug] VelocityVirtualSessionHandler activated in PLAY state for player {}", player.getUsername());
    keepAliveTask = player.getConnection().eventLoop().scheduleAtFixedRate(
        player::sendKeepAlive, 10, 10, TimeUnit.SECONDS);
  }

  @Override
  public void deactivated() {
    if (keepAliveTask != null) {
      keepAliveTask.cancel(false);
      keepAliveTask = null;
    }
  }

  @Override
  public boolean beforeHandle() {
    return connection.isClosed();
  }

  @Override
  public boolean handle(KeepAlivePacket packet) {
    return true;
  }

  public boolean handle(VirtualTeleportConfirmPacket packet) {
    LOGGER.info("[VirtualServer-Debug] Received VirtualTeleportConfirmPacket from player {}", player.getUsername());
    return true;
  }

  public boolean handle(VirtualChunkBatchReceivedPacket packet) {
    LOGGER.info("[VirtualServer-Debug] Received VirtualChunkBatchReceivedPacket from player {}", player.getUsername());
    return true;
  }

  @Override
  public boolean handle(ClientSettingsPacket packet) {
    player.setClientSettings(packet);
    return true;
  }

  @Override
  public boolean handle(LegacyChatPacket packet) {
    handleText(packet.getMessage());
    return true;
  }

  public boolean handle(VirtualMovePacket packet) {
    LOGGER.debug("[VirtualServer-Debug] Received VirtualMovePacket ({}, {}, {}) from player {}", packet.getX(), packet.getY(), packet.getZ(), player.getUsername());
    posX = packet.getX();
    posY = packet.getY();
    posZ = packet.getZ();
    yaw = packet.getYaw();
    pitch = packet.getPitch();
    notifyMove(packet.isOnGround());
    return true;
  }

  public boolean handle(VirtualMovePositionPacket packet) {
    posX = packet.getX();
    posY = packet.getY();
    posZ = packet.getZ();
    notifyMove(packet.isOnGround());
    return true;
  }

  public boolean handle(VirtualMoveRotationPacket packet) {
    yaw = packet.getYaw();
    pitch = packet.getPitch();
    notifyMove(packet.isOnGround());
    return true;
  }

  @Override
  public boolean handle(SessionPlayerCommandPacket packet) {
    handleCommand(packet.getCommand());
    return true;
  }

  @Override
  public boolean handle(SessionPlayerChatPacket packet) {
    handler().onChat(player, connection, packet.getMessage());
    return true;
  }

  @Override
  public boolean handle(KeyedPlayerCommandPacket packet) {
    handleCommand(packet.getCommand());
    return true;
  }

  @Override
  public boolean handle(KeyedPlayerChatPacket packet) {
    handler().onChat(player, connection, packet.getMessage());
    return true;
  }

  private void handleText(String message) {
    if (message.startsWith("/")) {
      handleCommand(message.substring(1));
    } else {
      handler().onChat(player, connection, message);
    }
  }

  private void handleCommand(String command) {
    if (!handler().onCommand(player, connection, command)) {
      proxy.getCommandManager().executeAsync(player, command);
    }
  }

  @Override
  public boolean handle(PluginMessagePacket packet) {
    ChannelIdentifier identifier = proxy.getChannelRegistrar().getFromId(packet.getChannel());
    if (identifier != null) {
      handler().onPluginMessage(player, connection, identifier,
          ByteBufUtil.getBytes(packet.content()));
    }
    return true;
  }

  private void notifyMove(boolean onGround) {
    handler().onMove(player, connection,
        new VirtualPosition(posX, posY, posZ, yaw, pitch, onGround));
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    if (buf.readableBytes() > 0) {
      int readerIndex = buf.readerIndex();
      int packetId = com.velocitypowered.proxy.protocol.ProtocolUtils.readVarInt(buf);
      LOGGER.info("[VirtualServer-Debug] Received unknown client play packet ID 0x{} (readableBytes={}) from player {}", Integer.toHexString(packetId), buf.readableBytes(), player.getUsername());
      buf.readerIndex(readerIndex);
    }
  }

  @Override
  public void disconnected() {
    deactivated();
    player.teardown();
  }
}