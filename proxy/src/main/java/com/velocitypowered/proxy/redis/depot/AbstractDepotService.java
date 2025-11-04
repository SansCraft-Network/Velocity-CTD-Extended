package com.velocitypowered.proxy.redis.depot;

import com.velocitypowered.proxy.redis.provider.RedisProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Elmar Blume - 18/05/2025
 */
public abstract non-sealed class AbstractDepotService<K, V extends DepotEntry<K, V>> implements DepotService<K, V> {

  protected final Depot<K, V> depot;

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
