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

package com.velocitypowered.proxy.redis.transaction;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.redis.packet.RedisPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a handler for a {@link Transaction}.
 *
 * @author Elmar Blume - 12/05/2025
 */
public abstract class TransactionHandler<T extends RedisPacket, R extends RedisPacket> {

  private final Class<? extends Transaction<T, R>> transactionClass;

  /**
   * Constructs a new {@link TransactionHandler}.
   *
   * @param transactionClass the class of the transaction
   */
  public TransactionHandler(@NotNull Class<? extends Transaction<T, R>> transactionClass) {
    this.transactionClass = transactionClass;
  }

  /**
   * Handle the redis-packet.
   *
   * @param packet the packet to handle
   * @return a newly created reply-packet
   */
  public abstract R handlePacket(T packet);

  /**
   * Create the reply-packet.
   *
   * @param redisPacket the packet to create the reply-packet from
   * @return {@code null} if the reply-packet is not needed, otherwise the reply-packet
   */
  public @Nullable R getReplyPacket(@NotNull RedisPacket redisPacket) {
    //noinspection unchecked - Use the abstract handlePacket method to get a reply-packet
    final R replyPacket = this.handlePacket((T) redisPacket);
    if (replyPacket == null) {
      return null;
    }


    // Adjust the transactionId and return the reply-packet
    Preconditions.checkNotNull(redisPacket.getTransactionId());
    replyPacket.setTransactionId(redisPacket.getTransactionId());
    replyPacket.setReply(true);

    return replyPacket;
  }

  /**
   * Get the class of the {@link Transaction} for this handler.
   *
   * @return the class of the transaction
   */
  public Class<? extends Transaction<T, R>> getTransactionClass() {
    return transactionClass;
  }
}
