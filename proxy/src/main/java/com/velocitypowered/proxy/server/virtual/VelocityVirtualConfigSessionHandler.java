/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.velocitypowered.proxy.server.virtual;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.ClientSettingsPacket;
import com.velocitypowered.proxy.protocol.packet.KeepAlivePacket;
import com.velocitypowered.proxy.protocol.packet.config.ActiveFeaturesPacket;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdatePacket;
import com.velocitypowered.proxy.protocol.packet.config.KnownPacksPacket;
import com.velocitypowered.proxy.protocol.packet.config.TagsUpdatePacket;
import com.velocitypowered.proxy.server.VelocityVirtualSessionHandler;
import net.kyori.adventure.key.Key;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Completes a no-backend 26.2 configuration sequence.
 * ViaProxy translates config packets for legacy clients.
 */
public final class VelocityVirtualConfigSessionHandler implements MinecraftSessionHandler {
  private static final Logger LOGGER = LogManager.getLogger(VelocityVirtualConfigSessionHandler.class);

  private final ConnectedPlayer player;
  private final VelocityVirtualSessionHandler playHandler;
  private final Runnable onPlay;

  public VelocityVirtualConfigSessionHandler(ConnectedPlayer player,
      VelocityVirtualSessionHandler playHandler, Runnable onPlay) {
    this.player = player;
    this.playHandler = playHandler;
    this.onPlay = onPlay;
  }

  @Override
  public void activated() {
    LOGGER.info("[VirtualServer-Debug] VelocityVirtualConfigSessionHandler activated for player {} (version {})", player.getUsername(), player.getProtocolVersion());
    player.getConnection().write(com.velocitypowered.proxy.server.virtual.engine.VirtualProtocolEngine.createKnownPacksPacket(ProtocolVersion.MINECRAFT_26_2));
    com.velocitypowered.proxy.server.virtual.engine.VirtualProtocolEngine.sendRegistrySync(player.getConnection()::write, ProtocolVersion.MINECRAFT_26_2);
    player.getConnection().write(new TagsUpdatePacket(VirtualProtocolBaseline.getCurrent().getTags()));
    player.getConnection().write(new ActiveFeaturesPacket(
        new Key[]{Key.key("minecraft", "vanilla")}));
    player.getConnection().write(FinishedUpdatePacket.INSTANCE);
    player.getConnection().flush();
    LOGGER.info("[VirtualServer-Debug] Sent FinishedUpdatePacket to player {}", player.getUsername());
  }

  @Override
  public boolean handle(KnownPacksPacket packet) {
    return true;
  }

  @Override
  public boolean handle(KeepAlivePacket packet) {
    return true;
  }

  @Override
  public boolean handle(ClientSettingsPacket packet) {
    player.setClientSettings(packet);
    return true;
  }

  @Override
  public boolean handle(FinishedUpdatePacket packet) {
    LOGGER.info("[VirtualServer-Debug] Received FinishedUpdatePacket from player {}, switching state to PLAY", player.getUsername());
    com.velocitypowered.proxy.server.virtual.via.VirtualViaCodec viaCodec =
        (com.velocitypowered.proxy.server.virtual.via.VirtualViaCodec) player.getConnection().getChannel()
            .pipeline().get(com.velocitypowered.proxy.network.Connections.VIRTUAL_VIA_CODEC);
    if (viaCodec != null) {
      viaCodec.getUser().getProtocolInfo().setClientState(com.viaversion.viaversion.api.protocol.packet.State.PLAY);
      viaCodec.getUser().getProtocolInfo().setServerState(com.viaversion.viaversion.api.protocol.packet.State.PLAY);
    }
    player.getConnection().setActiveSessionHandler(StateRegistry.PLAY, playHandler);
    onPlay.run();
    return true;
  }

  @Override
  public void disconnected() {
    player.teardown();
  }
}