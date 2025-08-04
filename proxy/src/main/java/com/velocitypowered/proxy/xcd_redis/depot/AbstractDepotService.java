package com.velocitypowered.proxy.xcd_redis.depot;

import com.velocitypowered.proxy.xcd_redis.provider.RedisProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * @author Elmar Blume - 18/05/2025
 */
public abstract non-sealed class AbstractDepotService<K, V extends DepotEntry<K, V>> implements DepotService<V> {

  protected final Depot<K, V> depot;

  public AbstractDepotService(Class<V> valueClass, @NotNull RedisProvider provider) {
    this.depot = provider.createDepot(valueClass);
  }

  @Override
  public @NotNull Collection<V> queryAll(Predicate<V> predicate) {
    return this.depot.values().stream().filter(predicate).toList();
  }
}
