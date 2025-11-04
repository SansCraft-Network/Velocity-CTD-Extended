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

package com.velocitypowered.proxy.redis.impl;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.command.builtin.VelocityCommand;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.redis.VelocityRedis;
import com.velocitypowered.proxy.redis.impl.transaction.VelocityGetPlayerPing;
import com.velocitypowered.proxy.redis.impl.transaction.VelocityReload;
import com.velocitypowered.proxy.redis.impl.transaction.VelocityTransferRemote;
import com.velocitypowered.proxy.redis.impl.transaction.VelocityUptime;
import com.velocitypowered.proxy.redis.packet.RedisPacket;
import com.velocitypowered.proxy.redis.packet.typed.ComponentPacket;
import com.velocitypowered.proxy.redis.transaction.Transaction;
import com.velocitypowered.proxy.redis.transaction.TransactionHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.translation.Argument;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Represents a registry that holds all {@link TransactionHandler} for the VelocityRedis module. An
 * internal {@link Delegate} is used to handle the data and create a new reply-packet from the data.
 * <p>
 * This registry is used to register the 'handle' section of the {@link Transaction} only. It's
 * completing and timeout behaviours are processed in the {@link Transaction} class itself.
 *
 * @author Elmar Blume - 15/05/2025
 */
public enum TransactionHandlerRegistry {

  /**
   * Handles the {@link VelocityGetPlayerPing} packet by replying with the player's ping if the player is on the proxy.
   */
  VELOCITY_GET_PLAYER_PING(VelocityGetPlayerPing.class, (server, packet) -> {
    // Ignore if the player is not on the proxy
    final Player player = server.getPlayer(packet.getPayload()).orElse(null);
    if (player == null) return null;

    // Create and return the response packet
    return new ComponentPacket(Component.translatable("velocity.command.ping.other", NamedTextColor.GREEN)
            .arguments(Component.text(player.getUsername()), Component.text(player.getPing())));
  }),

  /**
   * Handles the {@link VelocityUptime} packet by replying with the proxy's uptime if the packet is for this proxy.
   */
  VELOCITY_UPTIME(VelocityUptime.class, (server, packet) -> {
    // Ignore if the packet is not for this proxy
    if (!packet.getPayload().equalsIgnoreCase(server.getProxyId())) return null;

    // Create and return the response packet
    return new ComponentPacket(VelocityCommand.getUptimeComponent(server));
  }),

  /**
   * Handles the {@link VelocityReload} packet by replying with the proxy's uptime if the packet is for this proxy.
   */
  VELOCITY_RELOAD(VelocityReload.class, (server, packet) -> {
    // Ignore if the packet is not for this proxy
    if (!packet.getPayload().equalsIgnoreCase(server.getProxyId())) return null;

    // Reload the configuration and create the response component
    Component responseComponent;
    try {
      if (server.reloadConfiguration()) {
        responseComponent = Component.translatable("velocity.command.reload-success");
        server.getLogger().info("Reloaded Velocity configuration on remote request.");
      } else {
        responseComponent = Component.translatable("velocity.command.reload-failure");
        server.getLogger().error("Failed to reload Velocity configuration on remote request!");
      }
    } catch (Exception e) {
      responseComponent = Component.translatable("velocity.command.reload-failure");
      server.getLogger().error("Failed to reload Velocity configuration on remote request!", e);
    }

    // Create and return the response packet
    return new ComponentPacket(responseComponent);
  }),

  /**
   * Handles the {@link VelocityTransferRemote} packet by transferring a player to another remote/proxy
   */
  VELOCITY_TRANSFER_REMOTE(VelocityTransferRemote.class, (server, packet) -> {
    // Ignore if the player is not on the proxy
    final ConnectedPlayer connectedPlayer = (ConnectedPlayer) server.getPlayer(packet.getPayload()).orElse(null);
    if (connectedPlayer == null) return null;

    // Check if the player is on a compatible version
    if (connectedPlayer.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      return new ComponentPacket(Component.translatable("velocity.command.transfer.invalid-version")
              .arguments(Argument.string("player", connectedPlayer.getUsername())));
    }

    // Transfer the player to the requested host
    server.getScheduler().buildTask(VelocityVirtualPlugin.INSTANCE, () -> {
      connectedPlayer.transferToHost(new InetSocketAddress(packet.getIp(), packet.getPort()));
    }).delay(1, TimeUnit.SECONDS).schedule();

    return new ComponentPacket(Component.text("Transferring " + connectedPlayer.getUsername() + " to "
            + packet.getIp() + ":" + packet.getPort(), NamedTextColor.GREEN));
  }),
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
  <T extends RedisPacket, R extends RedisPacket> TransactionHandlerRegistry(
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
