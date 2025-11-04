/*
 * Copyright (C) 2025 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.redis.depot;

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
