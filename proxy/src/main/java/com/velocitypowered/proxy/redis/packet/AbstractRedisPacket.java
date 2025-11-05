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

package com.velocitypowered.proxy.redis.packet;

import com.velocitypowered.proxy.redis.VelocityRedis;
import com.velocitypowered.proxy.redis.packet.annotation.OneWayPacket;
import java.util.Objects;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstraction of {@link RedisPacket}.
 *
 * @author Elmar Blume - 08/05/2025
 */
public abstract sealed class AbstractRedisPacket implements RedisPacket permits EmptyPacket, GenericPacket {

  private final UUID packetId; // internal id
  private final String type;
  private final boolean oneWay;

  private boolean reply;

  private @MonotonicNonNull UUID transactionId;
  private @MonotonicNonNull String transactionType;

  /**
   * Constructs a new {@link AbstractRedisPacket}.
   */
  public AbstractRedisPacket() {
    this.packetId = UUID.randomUUID();
    this.type = this.getClass().getName();
    this.oneWay = this.getClass().isAnnotationPresent(OneWayPacket.class);
  }

  @Override
  public UUID getId() {
    return packetId;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public boolean isOneWay() {
    return oneWay;
  }

  @Override
  public boolean isReply() {
    return reply;
  }

  @Override
  public void setReply(boolean reply) {
    this.reply = reply;
  }

  @Override
  public @Nullable UUID getTransactionId() {
    return this.transactionId;
  }

  @Override
  public void setTransactionId(@NotNull UUID transactionId) {
    this.transactionId = transactionId;
  }

  @Override
  public @Nullable String getTransactionType() {
    return this.transactionType;
  }

  @Override
  public void setTransactionType(@NotNull String transactionType) {
    this.transactionType = transactionType;
  }

  @Override
  public void publish() {
    // Ensure Redis is initialized
    final VelocityRedis redis = VelocityRedis.INSTANCE;
    if (redis == null) {
      throw new IllegalStateException("Tried to publish packet without Redis being initialized.");
    }

    // Publish packet
    redis.getProvider().publish(this);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AbstractRedisPacket that = (AbstractRedisPacket) o;
    return Objects.equals(packetId, that.packetId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(packetId);
  }
}
