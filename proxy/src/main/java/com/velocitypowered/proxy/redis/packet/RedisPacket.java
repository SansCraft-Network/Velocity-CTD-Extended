package com.velocitypowered.proxy.redis.packet;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a packet that can be sent over redis
 *
 * @author Elmar Blume - 08/05/2025
 */
public sealed interface RedisPacket permits AbstractRedisPacket {

  /**
   * Get the unique id of the packet
   *
   * @return the unique id of the packet
   */
  UUID getId();

  /**
   * Get the type of the packet
   *
   * @return the type of the packet
   */
  String getType();

  /**
   * Determine if the packet is a one way packet
   *
   * @return true if the packet is a one way packet, false otherwise
   */
  boolean isOneWay();

  /**
   * Determine if the packet is a reply packet
   *
   * @return true if the packet is a reply packet, false otherwise
   */
  boolean isReply();

  /**
   * Set the packet as a reply packet
   *
   * @param reply true if the packet is a reply packet, false otherwise
   */
  void setReply(boolean reply);

  /**
   * Get the unique if of the {@link com.velocitypowered.proxy.redis.transaction.Transaction} this
   * packet is part of
   *
   * @return the unique id of the transaction
   */
  @Nullable UUID getTransactionId();

  /**
   * Set the unique id of the {@link com.velocitypowered.proxy.redis.transaction.Transaction} this
   * packet is part of
   *
   * @param transactionId the unique id of the transaction
   */
  void setTransactionId(@NotNull UUID transactionId);

  /**
   * Get the type of the {@link com.velocitypowered.proxy.redis.transaction.Transaction} this packet is part of
   *
   * @return the type of the transaction
   */
  @Nullable String getTransactionType();

  /**
   * Set the type of the {@link com.velocitypowered.proxy.redis.transaction.Transaction} this packet is part of
   *
   * @param transactionType the type of the transaction
   */
  void setTransactionType(@NotNull String transactionType);

  /**
   * Publish the packet to redis
   */
  void publish();

}
