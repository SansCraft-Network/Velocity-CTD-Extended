/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.velocitypowered.proxy.server.virtual;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.config.RegistrySyncPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.LongBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;

/**
 * 26.2 registries dynamically loaded from extracted vanilla data.
 */
public final class VirtualRegistryData262 implements VirtualProtocolBaseline {
  public static final VirtualRegistryData262 INSTANCE = new VirtualRegistryData262();

  private static final Map<String, List<String>> REGISTRIES = new LinkedHashMap<>();

  static {
    String resourcePath = "/com/velocitypowered/proxy/virtual/26.2/registries/registry_index.json";
    try (InputStream stream = VirtualRegistryData262.class.getResourceAsStream(resourcePath)) {
      if (stream == null) {
        throw new IllegalStateException("Missing virtual registry index resource: " + resourcePath);
      }
      JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
      for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
        List<String> entriesList = new ArrayList<>();
        JsonArray array = entry.getValue().getAsJsonArray();
        for (JsonElement element : array) {
          entriesList.add(element.getAsString());
        }
        REGISTRIES.put(entry.getKey(), entriesList);
      }
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to load virtual registry index", exception);
    }
  }

  private VirtualRegistryData262() {
  }

  @Override
  public com.velocitypowered.api.network.ProtocolVersion getProtocolVersion() {
    return com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_26_2;
  }

  @Override
  public void writeRegistrySync(java.util.function.Consumer<RegistrySyncPacket> consumer) {
    writeTo(consumer);
  }

  @Override
  public Map<String, Map<String, int[]>> getTags() {
    return tags();
  }

  public static void writeTo(java.util.function.Consumer<RegistrySyncPacket> consumer) {
    for (Map.Entry<String, List<String>> registry : REGISTRIES.entrySet()) {
      String regKey = registry.getKey();
      List<String> entries = registry.getValue();
      String folderName = regKey.substring("minecraft:".length()).replace('/', '_');

      ByteBuf payload = Unpooled.buffer();
      ProtocolUtils.writeString(payload, regKey);
      ProtocolUtils.writeVarInt(payload, entries.size());

      for (String entryName : entries) {
        String nameWithoutPrefix = entryName.substring("minecraft:".length());
        ProtocolUtils.writeString(payload, entryName);
        payload.writeBoolean(true);

        String path = "/com/velocitypowered/proxy/virtual/26.2/registries/"
            + folderName + "/" + nameWithoutPrefix + ".json";
        JsonElement element = readJson(path);

        ProtocolUtils.writeBinaryTag(payload,
            com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_26_2,
            toBinaryTag(element));
      }

      RegistrySyncPacket packet = new RegistrySyncPacket();
      packet.replace(payload);
      consumer.accept(packet);
    }
  }

  private static JsonElement readJson(String path) {
    try (InputStream stream = VirtualRegistryData262.class.getResourceAsStream(path)) {
      if (stream == null) {
        return new JsonObject();
      }
      return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    } catch (Exception exception) {
      return new JsonObject();
    }
  }

  public static Map<String, Map<String, int[]>> tags() {
    Map<String, Map<String, int[]>> tags = new HashMap<>();
    String resourcePath = "/com/velocitypowered/proxy/virtual/26.2/tags_index.json";
    try (InputStream stream = VirtualRegistryData262.class.getResourceAsStream(resourcePath)) {
      if (stream != null) {
        JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        for (Map.Entry<String, JsonElement> regEntry : root.entrySet()) {
          String regKey = regEntry.getKey();
          if (REGISTRIES.containsKey(regKey) || isStaticTagRegistry(regKey)) {
            Map<String, int[]> tagMap = new HashMap<>();
            JsonObject tagObj = regEntry.getValue().getAsJsonObject();
            for (Map.Entry<String, JsonElement> tagElem : tagObj.entrySet()) {
              tagMap.put(tagElem.getKey(), new int[]{0});
            }
            tags.put(regKey, tagMap);
          }
        }
      }
    } catch (Exception exception) {
      // fallback
    }
    tags.computeIfAbsent("minecraft:block", ignored -> new HashMap<>())
        .put("minecraft:infiniburn_overworld", new int[0]);
    return tags;
  }

  private static boolean isStaticTagRegistry(String regKey) {
    return regKey.equals("minecraft:block")
        || regKey.equals("minecraft:item")
        || regKey.equals("minecraft:entity_type")
        || regKey.equals("minecraft:fluid")
        || regKey.equals("minecraft:game_event")
        || regKey.equals("minecraft:banner_pattern")
        || regKey.equals("minecraft:cat_variant")
        || regKey.equals("minecraft:frog_variant")
        || regKey.equals("minecraft:instrument")
        || regKey.equals("minecraft:painting_variant")
        || regKey.equals("minecraft:point_of_interest_type");
  }

  private static BinaryTag toBinaryTag(JsonElement element) {
    if (element instanceof JsonObject object) {
      CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
      for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
        builder.put(entry.getKey(), toBinaryTag(entry.getValue()));
      }
      return builder.build();
    }
    if (element instanceof JsonArray array) {
      if (array.isEmpty()) {
        return ListBinaryTag.empty();
      }
      ListBinaryTag.Builder<BinaryTag> builder = ListBinaryTag.builder();
      for (JsonElement child : array) {
        builder.add(toBinaryTag(child));
      }
      return builder.build();
    }
    if (element instanceof JsonPrimitive primitive) {
      if (primitive.isBoolean()) {
        return ByteBinaryTag.byteBinaryTag((byte) (primitive.getAsBoolean() ? 1 : 0));
      }
      if (primitive.isString()) {
        return StringBinaryTag.stringBinaryTag(primitive.getAsString());
      }
      String number = primitive.getAsString();
      if (number.indexOf('.') >= 0 || number.indexOf('e') >= 0 || number.indexOf('E') >= 0) {
        return DoubleBinaryTag.doubleBinaryTag(primitive.getAsDouble());
      }
      long value = primitive.getAsLong();
      return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE
          ? IntBinaryTag.intBinaryTag((int) value) : LongBinaryTag.longBinaryTag(value);
    }
    throw new IllegalArgumentException("Unsupported registry JSON: " + element);
  }

  public static CompoundBinaryTag createDefaultDimensionCodec() {
    CompoundBinaryTag overworldElement = CompoundBinaryTag.builder()
        .putByte("piglin_safe", (byte) 0)
        .putByte("natural", (byte) 1)
        .putFloat("ambient_light", 0.0f)
        .putString("infiniburn", "#minecraft:infiniburn_overworld")
        .putByte("respawn_anchor_works", (byte) 0)
        .putByte("has_skylight", (byte) 1)
        .putByte("bed_works", (byte) 1)
        .putString("effects", "minecraft:overworld")
        .putByte("has_raids", (byte) 1)
        .putInt("min_y", -64)
        .putInt("height", 384)
        .putInt("logical_height", 384)
        .putDouble("coordinate_scale", 1.0)
        .putByte("ultrawarm", (byte) 0)
        .putByte("has_ceiling", (byte) 0)
        .build();

    CompoundBinaryTag dimensionEntry = CompoundBinaryTag.builder()
        .putString("name", "minecraft:overworld")
        .putInt("id", 0)
        .put("element", overworldElement)
        .build();

    ListBinaryTag dimensionList = ListBinaryTag.from(java.util.List.of(dimensionEntry));

    CompoundBinaryTag dimensionRegistry = CompoundBinaryTag.builder()
        .putString("type", "minecraft:dimension_type")
        .put("value", dimensionList)
        .build();

    CompoundBinaryTag biomeEffects = CompoundBinaryTag.builder()
        .putInt("fog_color", 12638463)
        .putInt("sky_color", 7907327)
        .putInt("water_color", 4159204)
        .putInt("water_fog_color", 329011)
        .build();

    CompoundBinaryTag biomeElement = CompoundBinaryTag.builder()
        .putString("precipitation", "rain")
        .putFloat("temperature", 0.8f)
        .putFloat("downfall", 0.4f)
        .put("effects", biomeEffects)
        .build();

    CompoundBinaryTag biomeEntry = CompoundBinaryTag.builder()
        .putString("name", "minecraft:plains")
        .putInt("id", 0)
        .put("element", biomeElement)
        .build();

    ListBinaryTag biomeList = ListBinaryTag.from(java.util.List.of(biomeEntry));

    CompoundBinaryTag biomeRegistry = CompoundBinaryTag.builder()
        .putString("type", "minecraft:worldgen/biome")
        .put("value", biomeList)
        .build();

    return CompoundBinaryTag.builder()
        .put("minecraft:dimension_type", dimensionRegistry)
        .put("minecraft:worldgen/biome", biomeRegistry)
        .build();
  }
}