/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.velocitypowered.proxy.server.virtual.via;

import com.viaversion.viabackwards.ViaBackwardsPlatformImpl;
import com.viaversion.viarewind.ViaRewindPlatformImpl;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import com.viaversion.viaversion.commands.ViaCommandHandler;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class VirtualViaManager {

  private static final Logger LOGGER = LogManager.getLogger(VirtualViaManager.class);
  private static boolean initialized = false;

  public static final ProtocolVersion PROTOCOL_26_2 = new ProtocolVersion(VersionType.SPECIAL, 776, -1, "26.2", null);

  private VirtualViaManager() {
  }

  public static synchronized void init(Path dataFolder) {
    if (initialized) {
      return;
    }
    LOGGER.info("[VirtualVia] Initializing embedded ViaVersion, ViaBackwards, and ViaRewind for virtual servers...");
    try {
      ProtocolVersion.register(PROTOCOL_26_2);

      ViaManagerImpl.initAndLoad(
          new VirtualViaVersionPlatform(dataFolder),
          new VirtualViaInjector(),
          new ViaCommandHandler(false),
          new VirtualViaPlatformLoader(),
          () -> {
            new ViaBackwardsPlatformImpl();
            new ViaRewindPlatformImpl();
          }
      );

      Via.getManager().getProtocolManager().registerProtocol(
          new Protocol26_2To1_21_4(), ProtocolVersion.v1_21_4, PROTOCOL_26_2);

      Protocol1_20_3To1_20_5.strictErrorHandling = false;
      initialized = true;
      LOGGER.info("[VirtualVia] Embedded ViaVersion stack successfully initialized with 26.2 baseline!");
    } catch (Throwable e) {
      LOGGER.error("[VirtualVia] Failed to initialize embedded ViaVersion stack!", e);
    }
  }

  public static boolean isInitialized() {
    return initialized;
  }

  public static java.util.Optional<com.viaversion.viaversion.api.connection.UserConnection> getUser(
      com.velocitypowered.proxy.connection.MinecraftConnection connection) {
    if (connection == null || connection.getChannel() == null) {
      return java.util.Optional.empty();
    }
    VirtualViaCodec viaCodec = (VirtualViaCodec) connection.getChannel().pipeline()
        .get(com.velocitypowered.proxy.network.Connections.VIRTUAL_VIA_CODEC);
    if (viaCodec != null) {
      return java.util.Optional.ofNullable(viaCodec.getUser());
    }
    return java.util.Optional.empty();
  }
}

