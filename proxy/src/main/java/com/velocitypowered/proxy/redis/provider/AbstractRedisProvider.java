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

package com.velocitypowered.proxy.redis.provider;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.proxy.redis.packet.RedisPacket;
import com.velocitypowered.proxy.redis.registration.RouteRegistration;
import com.velocitypowered.proxy.redis.transaction.Transaction;
import com.velocitypowered.proxy.redis.transaction.TransactionHandler;
import com.velocitypowered.proxy.redis.transaction.cache.TransactionCache;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Represents an abstract {@link RedisProvider} used to provide a common interface for all redis providers
 *
 * @author Elmar Blume - 08/05/2025
 * @see LettuceProvider
 */
public abstract sealed class AbstractRedisProvider implements RedisProvider permits LettuceProvider {
  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractRedisProvider.class);

  protected static final Cache<RedisPacket, Byte> HANDLED_PACKETS = CacheBuilder.newBuilder() // byte = dummy value
          .expireAfterWrite(10, TimeUnit.SECONDS).build();
  protected static final TransactionCache PENDING_TRANSACTIONS = new TransactionCache(
          (uuid, transaction) -> transaction.timeout());

  @MonotonicNonNull
  protected final Map<String, RouteRegistration<? extends RedisPacket>> routeRegistrations;
  @MonotonicNonNull
  protected final Map<String, TransactionHandler<?, ?>> transactionHandlers;

  public AbstractRedisProvider() {
    this.routeRegistrations = new HashMap<>();
    this.transactionHandlers = new HashMap<>();
  }

  @Override
  public <T extends RedisPacket> void publish(@NotNull Transaction<T, ?> transaction, int timeout, TimeUnit timeUnit) {
    final T sentPacket = transaction.getSentPacket();

    HANDLED_PACKETS.put(sentPacket, (byte) 0);
    PENDING_TRANSACTIONS.put(transaction, timeout, timeUnit);

    this.publish(sentPacket);
  }

  @Override
  public <T extends RedisPacket> void registerRoute(@NotNull RouteRegistration<T> routeRegistration) {
    final Class<T> packetClass = routeRegistration.getPacketClass();

    if (this.routeRegistrations.containsKey(packetClass.getName())) {
      LOGGER.debug("Route registration for '{}' already exists, overwriting", packetClass.getSimpleName());
    }

    this.routeRegistrations.put(packetClass.getName(), routeRegistration);
  }

  @Override
  public <T extends RedisPacket> void unregisterRoute(@NotNull Class<T> packetClass) {
    if (this.routeRegistrations.remove(packetClass.getName()) == null) {
      LOGGER.debug("Route registration for '{}' does not exist, ignoring", packetClass.getSimpleName());
    } else {
      LOGGER.debug("Unregistered route registration for '{}'", packetClass.getSimpleName());
    }
  }

  @Override
  public void registerTransaction(@NotNull TransactionHandler<?, ?> transactionHandler) {
    final Class<? extends Transaction<?, ?>> transactionClass = transactionHandler.getTransactionClass();

    if (this.transactionHandlers.containsKey(transactionClass.getName())) {
      LOGGER.debug("Transaction handler for '{}' already exists, overwriting", transactionClass.getSimpleName());
    }

    this.transactionHandlers.put(transactionClass.getName(), transactionHandler);
  }

  @Override
  public void unregisterTransaction(@NotNull Class<? extends Transaction<?, ?>> transactionClass) {
    if (this.transactionHandlers.remove(transactionClass.getName()) == null) {
      LOGGER.debug("Transaction handler for '{}' does not exist, ignoring", transactionClass.getSimpleName());
    } else {
      LOGGER.debug("Unregistered transaction handler for '{}'", transactionClass.getSimpleName());
    }
  }

  @Override
  public <T extends RedisPacket> @NotNull ImmutableList<RouteRegistration<T>> getRouteRegistrations() {
    //noinspection unchecked
    return this.routeRegistrations.values().stream()
            .map(routeRegistration -> (RouteRegistration<T>) routeRegistration)
            .collect(ImmutableList.toImmutableList());
  }
}
