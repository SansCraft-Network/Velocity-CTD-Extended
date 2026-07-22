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
import java.util.Set;

/**
 * Version-adaptive VirtualProtocol engine providing native packet serialization across all client versions.
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
    join.setDimensionInfo(new DimensionInfo("minecraft:overworld", "minecraft:overworld", true, false, version));
    join.setPartialHashedSeed(0);
    join.setPortalCooldown(0);
    join.setSeaLevel(63);
    join.setOnlineMode(isOnlineMode);
    join.setEnforcesSecureChat(false);

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
      join.setRegistry(VirtualRegistryData262.createDefaultDimensionCodec());
    }

    return join;
  }

  public static RespawnPacket createRespawnPacket(ProtocolVersion version, VirtualServerDefinition definition, boolean isOnlineMode) {
    return RespawnPacket.fromJoinGame(createJoinGamePacket(version, definition, isOnlineMode));
  }

  public static KnownPacksPacket createKnownPacksPacket(ProtocolVersion version) {
    String versionString = version.getVersionIntroducedIn();
    if (versionString == null || versionString.isEmpty()) {
      versionString = "26.2";
    }
    return new KnownPacksPacket(List.of(
        new KnownPacksPacket.KnownPack("minecraft", "core", versionString)
    ));
  }

  public static void sendRegistrySync(java.util.function.Consumer<com.velocitypowered.proxy.protocol.packet.config.RegistrySyncPacket> consumer, ProtocolVersion version) {
    if (version.equals(ProtocolVersion.MINECRAFT_26_2)) {
      VirtualRegistryData262.INSTANCE.writeRegistrySync(consumer);
      return;
    }

    if (version.lessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      io.netty.buffer.ByteBuf encodedRegistry = io.netty.buffer.Unpooled.buffer();
      com.velocitypowered.proxy.protocol.ProtocolUtils.writeBinaryTag(encodedRegistry, version, VirtualRegistryData262.createDefaultDimensionCodec());
      com.velocitypowered.proxy.protocol.packet.config.RegistrySyncPacket sync = new com.velocitypowered.proxy.protocol.packet.config.RegistrySyncPacket();
      sync.replace(encodedRegistry);
      consumer.accept(sync);
      return;
    }

    net.kyori.adventure.nbt.CompoundBinaryTag codec = VirtualRegistryData262.createDefaultDimensionCodec();
    for (String key : codec.keySet()) {
      net.kyori.adventure.nbt.CompoundBinaryTag entry = codec.getCompound(key);
      String type = entry.getString("type");
      net.kyori.adventure.nbt.ListBinaryTag values = entry.getList("value", net.kyori.adventure.nbt.BinaryTagTypes.COMPOUND);

      io.netty.buffer.ByteBuf registry = io.netty.buffer.Unpooled.buffer();
      com.velocitypowered.proxy.protocol.ProtocolUtils.writeString(registry, type);
      com.velocitypowered.proxy.protocol.ProtocolUtils.writeVarInt(registry, values.size());

      for (net.kyori.adventure.nbt.BinaryTag tag : values) {
        net.kyori.adventure.nbt.CompoundBinaryTag element = (net.kyori.adventure.nbt.CompoundBinaryTag) tag;
        com.velocitypowered.proxy.protocol.ProtocolUtils.writeString(registry, element.getString("name"));
        registry.writeBoolean(true);
        com.velocitypowered.proxy.protocol.ProtocolUtils.writeBinaryTag(registry, version, element.getCompound("element"));
      }

      com.velocitypowered.proxy.protocol.packet.config.RegistrySyncPacket sync = new com.velocitypowered.proxy.protocol.packet.config.RegistrySyncPacket();
      sync.replace(registry);
      consumer.accept(sync);
    }
  }
}
