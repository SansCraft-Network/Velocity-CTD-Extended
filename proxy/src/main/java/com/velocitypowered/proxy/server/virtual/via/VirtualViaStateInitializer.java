/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.velocitypowered.proxy.server.virtual.via;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.DoubleTag;
import com.viaversion.nbt.tag.FloatTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.DimensionData;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.data.entity.DimensionDataImpl;
import com.viaversion.viaversion.util.KeyMappings;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Pre-populates ViaVersion UserConnection tracking state for virtual server connections.
 * Prevents rewriter NPEs (EntityTracker, BiomeTracker, DimensionData, RegistryKeys) when translating
 * synthetic 26.2 virtual packets to legacy client versions.
 */
public final class VirtualViaStateInitializer {

  private static final Logger LOGGER = LogManager.getLogger(VirtualViaStateInitializer.class);

  private static final String[] ALL_REGISTRY_KEYS = new String[] {
      "minecraft:villager_trade", "villager_trade",
      "minecraft:cat_sound_variant", "cat_sound_variant",
      "minecraft:chicken_sound_variant", "chicken_sound_variant",
      "minecraft:cow_sound_variant", "cow_sound_variant",
      "minecraft:pig_sound_variant", "pig_sound_variant",
      "minecraft:wolf_sound_variant", "wolf_sound_variant",
      "minecraft:zombie_nautilus_variant", "zombie_nautilus_variant",
      "minecraft:timeline", "timeline",
      "minecraft:world_clock", "world_clock",
      "minecraft:dimension_type", "dimension_type",
      "minecraft:worldgen/biome", "worldgen/biome", "biome",
      "minecraft:jukebox_song", "jukebox_song",
      "minecraft:painting_variant", "painting_variant",
      "minecraft:trim_material", "trim_material",
      "minecraft:trim_pattern", "trim_pattern",
      "minecraft:banner_pattern", "banner_pattern",
      "minecraft:chat_type", "chat_type",
      "minecraft:damage_type", "damage_type",
      "minecraft:instrument", "instrument",
      "minecraft:enchantment", "enchantment",
      "minecraft:entity_type", "entity_type",
      "minecraft:block", "block",
      "minecraft:item", "item"
  };

  private VirtualViaStateInitializer() {
  }

  public static void initializeUser(UserConnection user, ProtocolVersion clientVersion) {
    if (user == null) {
      return;
    }

    try {
      CompoundTag overworldTag = new CompoundTag();
      overworldTag.put("height", new IntTag(384));
      overworldTag.put("min_y", new IntTag(-64));
      overworldTag.put("logical_height", new IntTag(384));
      overworldTag.put("coordinate_scale", new DoubleTag(1.0));
      overworldTag.put("ambient_light", new FloatTag(0.0f));
      overworldTag.put("infiniburn", new StringTag("#minecraft:infiniburn_overworld"));
      overworldTag.put("effects", new StringTag("minecraft:overworld"));

      DimensionData overworldData = new DimensionDataImpl(-64, overworldTag);
      Map<String, DimensionData> dimensions = Map.of(
          "minecraft:overworld", overworldData,
          "overworld", overworldData
      );

      KeyMappings dummyMappings = new KeyMappings(new String[]{"minecraft:dummy", "dummy"});

      for (EntityTracker tracker : user.getEntityTrackers()) {
        if (tracker == null) {
          continue;
        }
        tracker.setClientEntityId(1);
        tracker.setCurrentMinY(-64);
        tracker.setCurrentWorldSectionHeight(24);
        tracker.setCurrentWorld("minecraft:overworld");
        tracker.setCurrentDimensionId(0);
        tracker.setBiomesSent(1);

        try {
          tracker.setDimensions(dimensions);
        } catch (Throwable ignored) {
        }

        for (String regKey : ALL_REGISTRY_KEYS) {
          try {
            if (tracker.registryKeys(regKey) == null) {
              tracker.addRegistryKeys(regKey, dummyMappings);
            }
          } catch (Throwable ignored) {
          }
        }
      }

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

      LOGGER.info("[VirtualVia] Successfully initialized ViaVersion tracking state for client version {}", clientVersion);
    } catch (Throwable e) {
      LOGGER.warn("[VirtualVia] Failed to fully initialize ViaVersion state for client version {}", clientVersion, e);
    }
  }
}
