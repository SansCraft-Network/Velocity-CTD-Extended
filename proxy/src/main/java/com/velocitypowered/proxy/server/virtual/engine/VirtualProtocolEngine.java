/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.velocitypowered.proxy.server.virtual.engine;

import com.velocityctd.api.server.VirtualServerDefinition;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.protocol.packet.JoinGamePacket;
import com.velocitypowered.proxy.protocol.packet.RespawnPacket;
import com.velocitypowered.proxy.protocol.packet.config.KnownPacksPacket;
import com.velocitypowered.proxy.server.virtual.VirtualRegistryData262;
import com.velocitypowered.proxy.server.virtual.registry.VirtualDimension;
import java.util.List;

/**
 * Modern 26.2 VirtualProtocol engine. ViaProxy handles all protocol translations for legacy clients.
 */
public final class VirtualProtocolEngine {

  private VirtualProtocolEngine() {
  }

  public static JoinGamePacket createJoinGamePacket(ProtocolVersion version, VirtualServerDefinition definition, boolean isOnlineMode) {
    JoinGamePacket join = new JoinGamePacket();
    join.setEntityId(1);
    join.setIsHardcore(false);
    join.setGamemode((short) definition.getGameMode().ordinal());
    join.setPreviousGamemode((short) -1);
    join.setDimension(VirtualDimension.OVERWORLD.getModernId());
    join.setDifficulty((short) 0);
    join.setMaxPlayers(1);
    join.setLevelType("flat");
    join.setViewDistance(2);
    join.setSimulationDistance(2);
    join.setReducedDebugInfo(false);
    join.setShowRespawnScreen(true);
    join.setDoLimitedCrafting(false);
    join.setLevelNames(com.google.common.collect.ImmutableSet.of("minecraft:overworld"));
    join.setDimensionInfo(new DimensionInfo("minecraft:overworld", "minecraft:overworld", true, false, ProtocolVersion.MINECRAFT_26_2));
    join.setPartialHashedSeed(0);
    join.setPortalCooldown(0);
    join.setSeaLevel(63);
    join.setOnlineMode(isOnlineMode);
    join.setEnforcesSecureChat(false);
    return join;
  }

  public static RespawnPacket createRespawnPacket(ProtocolVersion version, VirtualServerDefinition definition, boolean isOnlineMode) {
    return RespawnPacket.fromJoinGame(createJoinGamePacket(version, definition, isOnlineMode));
  }

  public static KnownPacksPacket createKnownPacksPacket(ProtocolVersion version) {
    return new KnownPacksPacket(List.of(
        new KnownPacksPacket.KnownPack("minecraft", "core", "26.2")
    ));
  }

  public static void sendRegistrySync(java.util.function.Consumer<com.velocitypowered.proxy.protocol.packet.config.RegistrySyncPacket> consumer, ProtocolVersion version) {
    VirtualRegistryData262.INSTANCE.writeRegistrySync(consumer);
  }
}
