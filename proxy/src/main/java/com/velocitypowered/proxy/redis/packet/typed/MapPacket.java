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

package com.velocitypowered.proxy.redis.packet.typed;

import com.velocitypowered.proxy.redis.packet.GenericPacket;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elmar Blume - 12/05/2025
 */
public class MapPacket<K, V> extends GenericPacket<Map<K, V>> implements Map<K, V> {

  public MapPacket(Map<K, V> payload) {
    super(payload);
  }

  @Override
  public int size() {
    return this.payload.size();
  }

  @Override
  public boolean isEmpty() {
    return this.payload.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return this.payload.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return this.payload.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return this.payload.get(key);
  }

  @Override
  public @Nullable V put(K key, V value) {
    return this.payload.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return this.payload.remove(key);
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    this.payload.putAll(m);
  }

  @Override
  public void clear() {
    this.payload.clear();
  }

  @Override
  public @NotNull Set<K> keySet() {
    return this.payload.keySet();
  }

  @Override
  public @NotNull Collection<V> values() {
    return this.payload.values();
  }

  @Override
  public @NotNull Set<Entry<K, V>> entrySet() {
    return this.payload.entrySet();
  }
}
