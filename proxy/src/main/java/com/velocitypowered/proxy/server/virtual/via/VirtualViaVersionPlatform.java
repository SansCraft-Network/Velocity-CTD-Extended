/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.velocitypowered.proxy.server.virtual.via;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.platform.UserConnectionViaVersionPlatform;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.apache.logging.log4j.LogManager;

public class VirtualViaVersionPlatform extends UserConnectionViaVersionPlatform {

  private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(VirtualViaVersionPlatform.class);

  public VirtualViaVersionPlatform(Path dataFolder) {
    super(dataFolder.toFile());
  }

  @Override
  public Logger createLogger(String name) {
    return Logger.getLogger(name);
  }

  @Override
  public String getPlatformName() {
    return "VelocityVirtual";
  }

  @Override
  public String getPlatformVersion() {
    return "1.0.0";
  }

  @Override
  public boolean kickPlayer(UserConnection connection, String message) {
    LOGGER.info("[VirtualVia] Kicking player via ViaVersion: {}", message);
    return true;
  }

  @Override
  public void sendCustomPayload(UserConnection connection, String channel, byte[] message) {
    // Virtual servers don't forward payloads to backends
  }

  @Override
  public void sendCustomPayloadToClient(UserConnection connection, String channel, byte[] message) {
    // Virtual servers can send custom payloads directly if needed
  }

  @Override
  public JsonObject getDump() {
    JsonObject root = new JsonObject();
    root.addProperty("platform", getPlatformName());
    return root;
  }
}
