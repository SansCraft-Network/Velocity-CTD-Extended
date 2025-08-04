package com.velocitypowered.proxy.xcd_redis.depot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * A depot represents a map-like structure that stores a certain type of {@link V values}, indexed by a {@link String key} within
 * a Redis database/hash. It is used to store and retrieve objects in and from the database/hash.
 * <p>
 * Within VelocityRedis, the depot is used to store and retrieve objects like online players, server info from other proxies.
 *
 * @author Elmar Blume - 18/05/2025
 */
public interface Depot<K, V extends DepotEntry<K, V>> {

  boolean contains(@NotNull K key);

  @Nullable
  V get(K key);

  void upsert(@NotNull V value);

  @Nullable
  V remove(@NotNull K key);

  Collection<V> values();

  Collection<String> keys();

  default int size() {
    return keys().size();
  }
}
