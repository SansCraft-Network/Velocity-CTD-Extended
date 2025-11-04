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

import com.velocitypowered.proxy.redis.provider.RedisProvider;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * @author Elmar Blume - 18/05/2025
 */
public abstract non-sealed class AbstractDepotService<K, V extends DepotEntry<K, V>>
        implements DepotService<K, V> {

  protected final Depot<K, V> depot;

  /**
   * Constructs a new {@link AbstractDepotService}
   *
   * @param valueClass the class type of the value in the depot
   * @param provider the redis provider implementation instance
   */
  public AbstractDepotService(Class<V> valueClass, @NotNull RedisProvider provider) {
    this.depot = provider.createDepot(valueClass);
  }

  @Override
  public @Nullable V get(K key) {
    return this.depot.get(key);
  }

  @Override
  @Unmodifiable
  public @NotNull List<V> getAll() {
    return List.copyOf(this.depot.values());
  }

  @Override
  public @NotNull Collection<V> queryAll(Predicate<V> predicate) {
    return this.depot.values().stream().filter(predicate).toList();
  }

  @Override
  public void teardown() {
    // nothing by default
  }
}
