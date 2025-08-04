package com.velocitypowered.proxy.xcd_redis.transaction;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.xcd_redis.packet.RedisPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elmar Blume - 12/05/2025
 */
public abstract class TransactionHandler<T extends RedisPacket, R extends RedisPacket> {

  private final Class<? extends Transaction<T, R>> transactionClass;

  /**
   * Create a new transaction handler
   *
   * @param transactionClass the class of the transaction
   */
  public TransactionHandler(@NotNull Class<? extends Transaction<T, R>> transactionClass) {
    this.transactionClass = transactionClass;
  }

  /**
   * Handle the redis-packet
   *
   * @param packet the packet to handle
   * @return a newly created reply-packet
   */
  public abstract R handlePacket(T packet);

  /**
   * Create the reply-packet
   *
   * @param redisPacket the packet to create the reply-packet from
   * @return {@code null} if the reply-packet is not needed, otherwise the reply-packet
   */
  public @Nullable R getReplyPacket(@NotNull RedisPacket redisPacket) {
    //noinspection unchecked - Use the abstract handlePacket method to get a reply-packet
    final R replyPacket = this.handlePacket((T) redisPacket);
    if (replyPacket == null) return null;


    // Adjust the transactionId and return the reply-packet
    Preconditions.checkNotNull(redisPacket.getTransactionId());
    replyPacket.setTransactionId(redisPacket.getTransactionId());
    replyPacket.setReply(true);

    return replyPacket;
  }

  /**
   * Get the class of the {@link Transaction} for this handler
   *
   * @return the class of the transaction
   */
  public Class<? extends Transaction<T, R>> getTransactionClass() {
    return transactionClass;
  }
}
