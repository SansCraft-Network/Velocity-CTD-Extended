/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.velocitypowered.proxy.server.virtual;

import com.google.common.collect.ImmutableSet;
import com.velocityctd.api.server.VirtualServerDefinition;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.protocol.packet.JoinGamePacket;
import com.velocitypowered.proxy.protocol.packet.RespawnPacket;
import com.velocitypowered.proxy.protocol.packet.config.RegistrySyncPacket;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Interface representing a canonical baseline protocol version for native virtual servers.
 *
 * <p>Virtual server baseline data (registries, tags, spawn packets) is provided through this contract,
 * allowing seamless future version upgrades without altering session handler logic.</p>
 */
public interface VirtualProtocolBaseline {

  /**
   * The active canonical baseline implementation.
   */
  VirtualProtocolBaseline CURRENT = VirtualRegistryData262.INSTANCE;

  /**
   * Returns the protocol version associated with this baseline.
   *
   * @return the protocol version
   */
  ProtocolVersion getProtocolVersion();

  /**
   * Writes all registry synchronization packets for this baseline version.
   *
   * @param consumer receiver for registry sync packets
   */
  void writeRegistrySync(Consumer<RegistrySyncPacket> consumer);

  /**
   * Returns required tags for this baseline version.
   *
   * @return tag mapping
   */
  Map<String, Map<String, int[]>> getTags();

  /**
   * Creates a {@link JoinGamePacket} configured for this baseline version.
   *
   * @param definition virtual server definition
   * @param isOnlineMode whether online mode is enabled
   * @return populated JoinGamePacket
   */
  default JoinGamePacket createJoinGamePacket(VirtualServerDefinition definition, boolean isOnlineMode) {
    JoinGamePacket join = new JoinGamePacket();
    join.setEntityId(1);
    join.setIsHardcore(false);
    join.setGamemode((short) definition.getGameMode().ordinal());
    join.setPreviousGamemode((short) -1);
    join.setDimension(0);
    join.setMaxPlayers(1);
    join.setViewDistance(2);
    join.setSimulationDistance(2);
    join.setReducedDebugInfo(false);
    join.setShowRespawnScreen(true);
    join.setDoLimitedCrafting(false);
    join.setLevelNames(ImmutableSet.of("minecraft:overworld"));
    join.setDimensionInfo(new DimensionInfo("minecraft:overworld", "minecraft:overworld",
        true, false, getProtocolVersion()));
    join.setPartialHashedSeed(0);
    join.setPortalCooldown(0);
    join.setSeaLevel(63);
    join.setOnlineMode(isOnlineMode);
    join.setEnforcesSecureChat(false);
    return join;
  }

  /**
   * Creates a {@link RespawnPacket} configured for virtual-to-virtual transfers under this baseline version.
   *
   * @param definition virtual server definition
   * @param isOnlineMode whether online mode is enabled
   * @return populated RespawnPacket
   */
  default RespawnPacket createRespawnPacket(VirtualServerDefinition definition, boolean isOnlineMode) {
    return RespawnPacket.fromJoinGame(createJoinGamePacket(definition, isOnlineMode));
  }
}
