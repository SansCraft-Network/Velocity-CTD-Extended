package com.velocitypowered.proxy.redis.depot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Elmar Blume - 18/05/2025
 */
public sealed interface DepotService<K, V extends DepotEntry<K, V>> permits AbstractDepotService {

  @Nullable V get(K key);

  @NotNull
  @Unmodifiable
  List<V> getAll();

  @NotNull Collection<V> queryAll(Predicate<V> predicate);

  void teardown();

  default @Nullable V query(Predicate<V> predicate) {
    Collection<V> values = queryAll(predicate);
    if (values.isEmpty()) {
      return null;
    }
    return values.iterator().next();
  }

}
