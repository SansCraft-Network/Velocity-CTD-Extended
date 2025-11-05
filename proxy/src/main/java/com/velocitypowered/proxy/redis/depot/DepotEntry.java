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

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents an entry in a {@link Depot}.
 *
 * @author Elmar Blume - 18/05/2025
 */
public abstract class DepotEntry<K, T extends DepotEntry<K, T>> {

  private final K uniqueId;

  private transient @MonotonicNonNull Depot<K, T> depot;

  /**
   * Constructs a new {@link DepotEntry}.
   *
   * @param key the unique id of the entry
   */
  public DepotEntry(K key) {
    this.uniqueId = key;
  }

  /**
   * Updates or inserts the current instance into its associated {@link Depot}.
   */
  @SuppressWarnings("unchecked")
  public void upsert() {
    if (this.depot != null) {
      this.depot.upsert((T) this);
    }
  }

  /**
   * Removes this entry from its associated {@link Depot}, if it is currently present.
   * This operation is performed using the entry's unique identifier.
   */
  public void remove() {
    if (this.depot != null) {
      this.depot.remove(this.uniqueId);
    }
  }

  /**
   * Gets the unique identifier of this entry.
   *
   * @return the unique identifier of this entry
   */
  public K getUniqueId() {
    return uniqueId;
  }

  /**
   * Sets the associated {@link Depot}.
   *
   * @param depot the depot to set
   */
  @ApiStatus.Internal
  public void setDepot(Depot<K, T> depot) {
    if (depot != null) {
      this.depot = depot;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DepotEntry<?, ?> that = (DepotEntry<?, ?>) o;
    return Objects.equals(uniqueId, that.uniqueId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(uniqueId);
  }
}
