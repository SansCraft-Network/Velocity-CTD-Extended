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

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * @author Elmar Blume - 18/05/2025
 */
public abstract class DepotEntry<K, T extends DepotEntry<K, T>> {

  private final K uniqueId;

  private transient @MonotonicNonNull Depot<K, T> depot;

  public DepotEntry(K key) {
    this.uniqueId = key;
  }

  @SuppressWarnings("unchecked")
  public void upsert() {
    this.depot.upsert((T) this);
  }

  public void remove() {
    this.depot.remove(this.uniqueId);
  }

  public K getUniqueId() {
    return uniqueId;
  }

  @ApiStatus.Internal
  public void setDepot(Depot<K, T> depot) {
    if (depot == null) return;
    this.depot = depot;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    DepotEntry<?, ?> that = (DepotEntry<?, ?>) o;
    return Objects.equals(uniqueId, that.uniqueId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(uniqueId);
  }
}
