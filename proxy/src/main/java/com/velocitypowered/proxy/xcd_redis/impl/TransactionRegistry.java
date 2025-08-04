package com.velocitypowered.proxy.xcd_redis.impl;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.xcd_redis.VelocityRedis;
import com.velocitypowered.proxy.xcd_redis.impl.transaction.VelocityGetPlayerPing;
import com.velocitypowered.proxy.xcd_redis.packet.RedisPacket;
import com.velocitypowered.proxy.xcd_redis.packet.typed.ComponentPacket;
import com.velocitypowered.proxy.xcd_redis.transaction.Transaction;
import com.velocitypowered.proxy.xcd_redis.transaction.TransactionHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Represents a registry that holds all {@link TransactionHandler} for the VelocityRedis module. An
 * internal {@link Delegate} is used to handle the data and create a new reply-packet from the data.
 * <p>
 * This registry is used to register the 'handle' section of the {@link Transaction} only. It's
 * completing and timeout behaviours are processed in the {@link Transaction} class itself.
 *
 * @author Elmar Blume - 15/05/2025
 */
public enum TransactionRegistry {

  /**
   * Execute /ping -> velocity.command.ping.other" component
   */
  GET_PLAYER_PING(VelocityGetPlayerPing.class, (server, packet) ->
          server.getPlayer(packet.getPayload()).map(player -> new ComponentPacket(
                          Component.translatable("velocity.command.ping.other", NamedTextColor.GREEN)
                                  .arguments(Component.text(player.getUsername()), Component.text(player.getPing()))
                  ))
                  .orElse(null)),
  ;

  private final TransactionHandler<?, ?> transactionHandler;

  /**
   * Create a new {@link Transaction} registry, which holds the {@link TransactionHandler}
   *
   * @param transactionClass the class of the {@link Transaction}
   * @param delegate         the delegate to handle the data, which is passed to the {@link TransactionHandler}
   * @param <T>              the type of the data (extends {@link Record})
   * @param <R>              the type of the reply-packet (extends {@link RedisPacket})
   */
  <T extends RedisPacket, R extends RedisPacket> TransactionRegistry(
          Class<? extends Transaction<T, R>> transactionClass,
          Delegate<T, R> delegate) {
    this.transactionHandler = new TransactionHandler<>(transactionClass) {
      @Override
      public R handlePacket(T packet) {
        return delegate.handleData(VelocityRedis.INSTANCE.getServer(), packet);
      }
    };
  }

  /**
   * Get the {@link TransactionHandler} for this {@link Transaction}
   *
   * @return the transaction handler
   */
  public TransactionHandler<?, ?> getTransactionHandler() {
    return transactionHandler;
  }

  /**
   * Functional interface for handling data in a transaction, used for
   * creating a new reply-packet from the data
   *
   * @param <T> the type of the data (extends {@link Record})
   * @param <R> the type of the reply-packet (extends {@link RedisPacket})
   */
  @FunctionalInterface
  public interface Delegate<T extends RedisPacket, R extends RedisPacket> {
    R handleData(VelocityServer server, T data);
  }
}
