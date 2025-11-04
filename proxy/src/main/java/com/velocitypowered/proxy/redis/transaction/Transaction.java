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

import com.velocitypowered.proxy.redis.VelocityRedis;
import com.velocitypowered.proxy.redis.packet.RedisPacket;
import com.velocitypowered.proxy.redis.provider.RedisProvider;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a transaction process that has a {@link T sent-packet} and a {@link R reply-packet}. The transaction's
 * behaviour can be configured using the {@link Transaction#onTimeout(Consumer)} and {@link Transaction#onComplete(Consumer)}
 *
 * @author Elmar Blume - 12/05/2025
 */
public class Transaction<T extends RedisPacket, R extends RedisPacket> {
  public static final int DEFAULT_TIMEOUT = 5;
  public static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

  private static final Logger LOGGER = LoggerFactory.getLogger(Transaction.class);

  private final UUID transactionId;
  private final T sentPacket;

  private int timeout = 5;
  private TimeUnit timeUnit = TimeUnit.SECONDS;

  private Consumer<T> timeoutConsumer; // called when the transaction times out
  private Consumer<R> completeConsumer; // called when the transaction receives a reply

  /**
   * Constructs a new {@link Transaction} given an instance of the required sent-packet
   *
   * @param sentPacket the sent-packet instance to publish
   */
  public Transaction(@NotNull T sentPacket) {
    this.transactionId = UUID.randomUUID();
    this.sentPacket = sentPacket;
    this.sentPacket.setTransactionId(this.transactionId);
    this.sentPacket.setTransactionType(this.getClass().getName());
  }

  /**
   * Publish the {@link Transaction} to all subscribers on the redis
   *
   * @param provider the {@link RedisProvider} to publish the transaction to
   * @return itself for chaining
   *
   * @see RedisProvider#publish(Transaction, int, TimeUnit)
   */
  public Transaction<T, R> publish(@NotNull RedisProvider provider) {
    provider.publish(this);
    return this;
  }

  /**
   * Publish the {@link Transaction} to all subscribers on the redis
   *
   * @return itself for chaining
   *
   * @see RedisProvider#publish(Transaction, int, TimeUnit)
   */
  public Transaction<T, R> publish() {
    final RedisProvider provider = VelocityRedis.INSTANCE.getProvider();
    if (provider == null) {
      throw new IllegalStateException("No redis instance has been provided");
    }

    return publish(provider);
  }

  /**
   * Set the timeout for the {@link Transaction}
   *
   * @param timeout  the timeout in the given time unit
   * @param timeUnit the time unit of the timeout argument
   * @return itself for chaining
   */
  public Transaction<T, R> setTimeout(int timeout, TimeUnit timeUnit) {
    this.timeout = timeout;
    this.timeUnit = timeUnit;
    return this;
  }

  /**
   * Set the timeout consumer for the {@link Transaction}
   *
   * @param timeoutConsumer the consumer to call when the transaction times out
   * @return itself for chaining
   */
  public Transaction<T, R> onTimeout(Consumer<T> timeoutConsumer) {
    this.timeoutConsumer = timeoutConsumer;
    return this;
  }

  /**
   * Set the reply consumer for the {@link Transaction}
   *
   * @param completeConsumer the consumer to call when the transaction receives a reply
   * @return itself for chaining
   */
  public Transaction<T, R> onComplete(Consumer<R> completeConsumer) {
    this.completeConsumer = completeConsumer;
    return this;
  }

  /**
   * Complete the {@link Transaction} by accepting the {@link Transaction#onComplete(Consumer)} consumer
   *
   * @param replyPacket the reply packet to use as acceptance
   */
  @SuppressWarnings("unchecked")
  public void complete(RedisPacket replyPacket) {
    if (this.completeConsumer != null) {
      try {
        this.completeConsumer.accept((R) replyPacket);
      } catch (ClassCastException ignored) {
        LOGGER.warn("Reply packet contains invalid type: {}", replyPacket.getClass());
      }
    } else {
      this.timeout();
    }
  }

  /**
   * Timeout the {@link Transaction} by accepting the {@link Transaction#onTimeout(Consumer)}
   *
   * @apiNote This method is called automatically when the transaction times out,
   * which can be configured using {@link Transaction#setTimeout(int, TimeUnit)}
   */
  public void timeout() {
    if (this.timeoutConsumer != null) {
      this.timeoutConsumer.accept(this.sentPacket);
    }
  }

  /**
   * Get the unique id of the {@link Transaction}
   *
   * @return the unique id of the transaction
   */
  public UUID getTransactionId() {
    return transactionId;
  }

  /**
   * Get the sent-packet of the {@link Transaction}
   *
   * @return the sent-packet of the transaction
   */
  public T getSentPacket() {
    return sentPacket;
  }

  /**
   * Get the timeout of the {@link Transaction}
   *
   * @return the timeout of the transaction
   */
  public int getTimeout() {
    return timeout;
  }

  /**
   * Get the time unit of the {@link Transaction}
   *
   * @return the time unit of the transaction
   */
  public TimeUnit getTimeUnit() {
    return timeUnit;
  }
}
