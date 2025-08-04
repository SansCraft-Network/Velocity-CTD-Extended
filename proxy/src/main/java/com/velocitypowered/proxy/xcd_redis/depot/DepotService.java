package com.velocitypowered.proxy.xcd_redis.depot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * @author Elmar Blume - 18/05/2025
 */
public sealed interface DepotService<V extends DepotEntry<?, V>> permits AbstractDepotService {

  @NotNull Collection<V> queryAll(Predicate<V> predicate);

  default @Nullable V query(Predicate<V> predicate) {
    Collection<V> values = queryAll(predicate);
    if (values.isEmpty()) {
      return null;
    }
    return values.iterator().next();
  }

}
