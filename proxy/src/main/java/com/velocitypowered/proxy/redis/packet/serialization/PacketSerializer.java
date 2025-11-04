package com.velocitypowered.proxy.redis.packet.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.velocitypowered.proxy.redis.packet.GenericPacket;
import com.velocitypowered.proxy.redis.packet.RedisPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;

/**
 * Represents a utility class for serializing {@link RedisPacket} objects to JSON strings using {@link Gson}
 *
 * @author Elmar Blume - 09/05/2025
 */
public final class PacketSerializer {

  public static final Gson GSON = new GsonBuilder()
          .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
          .disableHtmlEscaping()
          .serializeNulls()
          .create();

  /**
   * Serializes a {@link RedisPacket} to a JSON string using {@link Gson}
   *
   * @param packet the packet to serialize as a JSON string
   * @param <T>    the type of the packet
   * @return the JSON string representation of the packet
   */
  @NotNull
  public static <T extends RedisPacket> String serialize(@NotNull T packet) {
    return PacketSerializer.GSON.toJson(packet);
  }

  /**
   * Deserializes a JSON string to a {@link RedisPacket} object using {@link Gson}
   *
   * @param serializedPacket the JSON string to deserialize
   * @param <T>              the class of the packet
   * @return the deserialized {@link RedisPacket} object, or null if the deserialization fails
   */
  @Nullable
  public static <T extends RedisPacket> T deserialize(@NotNull String serializedPacket, Class<T> packetClass) {
    return PacketSerializer.GSON.fromJson(serializedPacket, packetClass);
  }

  /**
   * Deserializes a JSON string to a {@link RedisPacket} object using {@link Gson}
   *
   * @param serializedPacket the JSON string to deserialize
   * @param <T>              the type of the packet
   * @return the deserialized {@link RedisPacket} object, or null if the deserialization fails
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public static <T extends RedisPacket> T deserialize(@NotNull String serializedPacket) {
    final RedisPacket redisPacket = PacketSerializer.GSON.fromJson(serializedPacket, GenericPacket.class);
    if (redisPacket == null) return null;

    try {
      final Class<T> type = (Class<T>) Class.forName(redisPacket.getType());

      // Attempt to deserialize the packet using the type
      return PacketSerializer.GSON.fromJson(serializedPacket, type);
    } catch (ClassNotFoundException ignored) {
      // Fallback to standard deserialization
      return (T) redisPacket;
    }
  }

  /**
   * Prepares a JSON string for serialization by checking if the serialized packet contains a type field
   *
   * @param serializedPacket the JSON string to prepare
   * @return the type field of the serialized packet, or null if the type field is not present
   */
  @Nullable
  public static String prepare(@NotNull String serializedPacket) {
    final JsonObject jsonObject = PacketSerializer.GSON.fromJson(serializedPacket, JsonObject.class);
    if (jsonObject == null || !jsonObject.has("type")) return null;

    return jsonObject.getAsJsonPrimitive("type").getAsString();
  }
}
