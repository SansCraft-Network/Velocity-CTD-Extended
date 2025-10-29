package com.velocitypowered.proxy.xcd_redis.provider;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.proxy.xcd_redis.depot.Depot;
import com.velocitypowered.proxy.xcd_redis.depot.DepotEntry;
import com.velocitypowered.proxy.xcd_redis.packet.RedisPacket;
import com.velocitypowered.proxy.xcd_redis.registration.RouteRegistration;
import com.velocitypowered.proxy.xcd_redis.transaction.Transaction;
import com.velocitypowered.proxy.xcd_redis.transaction.TransactionHandler;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * @author Elmar Blume - 08/05/2025
 */
public sealed interface RedisProvider permits AbstractRedisProvider {

  String CHANNEL = "velocityredis.xcd";

  /**
   * Restart the redis provider
   */
  void restart();

  /**
   * Disconnect the redis provider
   */
  void disconnect();

  /**
   * Publish a {@link RedisPacket} to the channel on the redis
   *
   * @param packet the packet to publish
   * @param <T>    the type of the packet
   */
  <T extends RedisPacket> void publish(@NotNull T packet);

  /**
   * Publish a {@link Transaction} to all subscribers on the redis
   *
   * @param transaction the transaction to publish
   * @param timeout     the timeout in seconds (default = 5)
   * @param timeUnit    the time unit of the timeout (default = seconds)
   * @param <T>         the type of the sent-packet
   */
  <T extends RedisPacket> void publish(@NotNull Transaction<T, ?> transaction, int timeout, TimeUnit timeUnit);

  /**
   * Publish a {@link Transaction} to all subscribers on the redis
   *
   * @param transaction the transaction to publish
   * @param <T>         the type of the sent-packet
   * @see #publish(Transaction, int, TimeUnit)
   */
  default <T extends RedisPacket> void publish(@NotNull Transaction<T, ?> transaction) {
    publish(transaction, transaction.getTimeout(), transaction.getTimeUnit());
  }

  /**
   * Register a {@link RouteRegistration} for a specific packet class
   *
   * @param routeRegistration the route registration to register
   * @param <T>               the type of the packet
   */
  <T extends RedisPacket> void registerRoute(@NotNull RouteRegistration<T> routeRegistration);

  /**
   * Unregister a {@link RouteRegistration} for a specific packet class, if it exists
   *
   * @param packetClass the class of the packet to unregister
   * @param <T>         the type of the packet
   */
  <T extends RedisPacket> void unregisterRoute(@NotNull Class<T> packetClass);

  /**
   * Get an immutable list of all route registrations
   *
   * @param <T> the type of the packet
   * @return the immutable list of all route registrations
   */
  <T extends RedisPacket> @NotNull ImmutableList<RouteRegistration<T>> getRouteRegistrations();

  /**
   * Register a {@link TransactionHandler} for a specific transaction class
   *
   * @param transactionHandler the transaction handler to register
   */
  void registerTransaction(@NotNull TransactionHandler<?, ?> transactionHandler);

  /**
   * Unregister a {@link TransactionHandler} for a specific transaction class, if it exists
   *
   * @param transactionClass the class of the transaction to unregister
   */
  void unregisterTransaction(@NotNull Class<? extends Transaction<?, ?>> transactionClass);

  /**
   * Create a {@link Depot} that can hold objects of a specific type
   *
   * @param <K> the type of the key in the depot
   * @param <V> the type of the object value in the depot
   * @return a new depot instance
   */
  <K, V extends DepotEntry<K, V>> @NotNull Depot<K, V> createDepot(Class<V> valueClass);

  /**
   * Check if the redis provider is connected
   *
   * @return true if the redis provider is connected, false otherwise
   */
  boolean isConnected();
}
