package com.velocitypowered.proxy.xcd_redis.packet;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.xcd_redis.VelocityRedis;
import com.velocitypowered.proxy.xcd_redis.packet.annotation.OneWayPacket;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Abstraction of {@link RedisPacket}
 *
 * @author Elmar Blume - 08/05/2025
 */
public sealed abstract class AbstractRedisPacket implements RedisPacket permits EmptyPacket, GenericPacket {

  private final UUID packetId; // internal id
  private final String type;
  private final boolean oneWay;

  private boolean reply;

  private @MonotonicNonNull UUID transactionId;
  private @MonotonicNonNull String transactionType;

  /**
   * Constructs a new {@link AbstractRedisPacket}
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
    if (redis == null) throw new IllegalStateException("Tried to publish packet without Redis being initialized.");

    // Publish packet
    redis.getProvider().publish(this);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    AbstractRedisPacket that = (AbstractRedisPacket) o;
    return Objects.equals(packetId, that.packetId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(packetId);
  }
}
