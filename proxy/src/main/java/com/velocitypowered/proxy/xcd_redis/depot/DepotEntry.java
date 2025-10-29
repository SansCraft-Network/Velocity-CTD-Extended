package com.velocitypowered.proxy.xcd_redis.depot;

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
