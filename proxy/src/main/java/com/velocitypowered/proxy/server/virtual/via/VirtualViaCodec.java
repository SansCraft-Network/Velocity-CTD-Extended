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
import com.viaversion.viaversion.platform.ViaCodecHandler;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VirtualViaCodec extends ViaCodecHandler {

  private static final Logger LOGGER = LogManager.getLogger(VirtualViaCodec.class);
  private final UserConnection user;

  public VirtualViaCodec(UserConnection user) {
    super(user);
    this.user = user;
  }

  private static final com.viaversion.viaversion.util.KeyMappings DUMMY_MAPPINGS =
      new com.viaversion.viaversion.util.KeyMappings(new String[]{"minecraft:dummy", "dummy"});
  private static final String[] REQUIRED_REGISTRIES = new String[] {
      "minecraft:cat_sound_variant", "cat_sound_variant",
      "minecraft:chicken_sound_variant", "chicken_sound_variant",
      "minecraft:cow_sound_variant", "cow_sound_variant",
      "minecraft:pig_sound_variant", "pig_sound_variant",
      "minecraft:wolf_sound_variant", "wolf_sound_variant",
      "minecraft:zombie_nautilus_variant", "zombie_nautilus_variant",
      "minecraft:timeline", "timeline",
      "minecraft:world_clock", "world_clock",
      "minecraft:worldgen/configured_feature", "worldgen/configured_feature",
      "minecraft:worldgen/density_function", "worldgen/density_function",
      "minecraft:worldgen/flat_level_generator_preset", "worldgen/flat_level_generator_preset",
      "minecraft:worldgen/multi_noise_biome_source_parameter_list", "worldgen/multi_noise_biome_source_parameter_list",
      "minecraft:worldgen/noise", "worldgen/noise",
      "minecraft:worldgen/noise_settings", "worldgen/noise_settings",
      "minecraft:worldgen/placed_feature", "worldgen/placed_feature",
      "minecraft:worldgen/processor_list", "worldgen/processor_list",
      "minecraft:worldgen/structure", "worldgen/structure",
      "minecraft:worldgen/structure_set", "worldgen/structure_set",
      "minecraft:worldgen/template_pool", "worldgen/template_pool",
      "minecraft:worldgen/world_preset", "worldgen/world_preset"
  };

  public UserConnection getUser() {
    return user;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    try {
      super.channelRead(ctx, msg);
    } catch (Throwable e) {
      LOGGER.error("[VirtualVia] Error in ViaVersion decoder handling incoming packet", e);
      throw e;
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, io.netty.channel.ChannelPromise promise) throws Exception {
    if (user != null) {
      try {
        com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.storage.RegistryDataStorage storage =
            user.get(com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.storage.RegistryDataStorage.class);
        if (storage != null && storage.dimensionKeys() == null) {
          storage.setDimensionKeys(new String[]{"minecraft:overworld"});
        }
      } catch (Throwable ignored) {
      }

      try {
        com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.storage.SpawnPositionStorage spawnStorage =
            user.get(com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.storage.SpawnPositionStorage.class);
        if (spawnStorage == null) {
          spawnStorage = new com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.storage.SpawnPositionStorage();
          user.put(spawnStorage);
        }
        if (spawnStorage.getSpawnPosition() == null) {
          spawnStorage.setSpawnPosition(com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.storage.SpawnPositionStorage.DEFAULT_SPAWN_POSITION);
        }
      } catch (Throwable ignored) {
      }

      try {
        com.viaversion.viabackwards.protocol.v1_20_2to1_20.storage.ConfigurationPacketStorage configStorage =
            user.get(com.viaversion.viabackwards.protocol.v1_20_2to1_20.storage.ConfigurationPacketStorage.class);
        if (configStorage == null) {
          configStorage = new com.viaversion.viabackwards.protocol.v1_20_2to1_20.storage.ConfigurationPacketStorage();
          user.put(configStorage);
        }
        try {
          configStorage.registry();
        } catch (Throwable e) {
          configStorage.setRegistry(new com.viaversion.nbt.tag.CompoundTag());
        }
      } catch (Throwable ignored) {
      }

      for (com.viaversion.viaversion.api.data.entity.EntityTracker tracker : user.getEntityTrackers()) {
        if (tracker != null) {
          for (String reg : REQUIRED_REGISTRIES) {
            try {
              if (tracker.registryKeys(reg) == null) {
                tracker.addRegistryKeys(reg, DUMMY_MAPPINGS);
              }
            } catch (Throwable ignored) {
            }
          }
        }
      }
    }
    try {
      LOGGER.info("[VirtualVia-DIAG] write() called: msgType={}, isByteBuf={}, viaClientState={}, viaServerState={}, shouldTransform={}, clientVersion={}, serverVersion={}",
          msg.getClass().getSimpleName(),
          (msg instanceof io.netty.buffer.ByteBuf),
          user != null ? user.getProtocolInfo().getClientState() : "null",
          user != null ? user.getProtocolInfo().getServerState() : "null",
          user != null ? user.shouldTransformPacket() : "null",
          user != null ? user.getProtocolInfo().protocolVersion() : "null",
          user != null ? user.getProtocolInfo().serverProtocolVersion() : "null");
      if (msg instanceof io.netty.buffer.ByteBuf buf && buf.readableBytes() > 0) {
        int readerIdx = buf.readerIndex();
        int packetId = com.velocitypowered.proxy.protocol.ProtocolUtils.readVarInt(buf);
        buf.readerIndex(readerIdx);
        LOGGER.info("[VirtualVia-DIAG] ByteBuf packetId=0x{}, readableBytes={}", Integer.toHexString(packetId), buf.readableBytes());
      }
      super.write(ctx, msg, promise);
    } catch (Throwable e) {
      LOGGER.warn("[VirtualVia] Suppressed non-critical outgoing translation error for virtual client: {}", e.getMessage());
      promise.setSuccess();
    }
  }
}
