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

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.queue.Queue;
import com.velocitypowered.proxy.queue.model.QueuePlayer;
import com.velocitypowered.proxy.queue.redis.packet.VelocityQueueTransfer;
import com.velocitypowered.proxy.redis.VelocityRedis;
import com.velocitypowered.proxy.redis.impl.packet.VelocityActionBar;
import com.velocitypowered.proxy.redis.impl.packet.VelocityAlert;
import com.velocitypowered.proxy.redis.impl.packet.VelocityKick;
import com.velocitypowered.proxy.redis.impl.packet.VelocityMessage;
import com.velocitypowered.proxy.redis.impl.packet.VelocitySudo;
import com.velocitypowered.proxy.redis.impl.packet.VelocitySwitchServer;
import com.velocitypowered.proxy.redis.packet.RedisPacket;
import com.velocitypowered.proxy.redis.registration.ConsumerRouteRegistration;
import com.velocitypowered.proxy.redis.registration.RouteRegistration;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a registry that holds all {@link RouteRegistration} for the VelocityRedis module. An
 * internal {@link Function} is used to handle the data and create a response from the data.
 *
 * @author Elmar Blume - 17/05/2025
 */
public enum RouteRegistry {

  /**
   * Handles the {@link VelocityAlert} packet by sending a message to all players on the proxy.
   */
  VELOCITY_ALERT(VelocityAlert.class, PacketBehaviour.SEND_COMPONENT::behave),

  /**
   * Handles the {@link VelocitySwitchServer} packet by switching the player to the specified server.
   */
  VELOCITY_SWITCH_SERVER(VelocitySwitchServer.class, (server, packet) -> {
    // Ignore if the player is not on the proxy
    final Player player = server.getPlayer(packet.getUsername()).orElse(null);
    if (player == null) return;

    // Create a connection request to the target server if it exists
    server.getServer(packet.getServerName()).ifPresent(targetServer ->
            player.createConnectionRequest(targetServer).connectWithIndication());
  }),

  /**
   * Handles the {@link VelocityMessage} packet by sending a message to the specified target.
   */
  VELOCITY_MESSAGE(VelocityMessage.class, (server, packet) -> {
    // Send the message
    packet.sendMessage(server);
  }),

  /**
   * Handles the {@link VelocityActionBar} packet by sending an action bar to the specified target.
   */
  VELOCITY_ACTION_BAR(VelocityActionBar.class, (server, packet) -> {
    final Component component = packet.deserialize();
    if (component == null) return;

    // Send the message to the target
    server.getPlayer(packet.getUniqueId()).ifPresent(player -> player.sendActionBar(component));
  }),

  /**
   * Handles the {@link VelocitySudo} packet by letting the specified player execute a command or chat message.
   */
  VELOCITY_SUDO(VelocitySudo.class, (server, packet) -> {
    // Ignore if the player is not on the proxy
    final Player player = server.getPlayer(packet.getPayload()).orElse(null);
    if (player == null) return;

    // Execute a command if applicable
    final String message = packet.getMessage();
    if (message.startsWith("/")) {
      final String command = message.split(" ")[0].substring(1);
      if (server.getCommandManager().hasCommand(command)) {
        server.getCommandManager().executeAsync(player, command);
        return;
      }
    }

    // Otherwise spoof a chat input
    player.spoofChatInput(message);
  }),

  /**
   * Handles the {@link VelocityKick} packet by kicking the specified player with a reason.
   */
  VELOCITY_KICK(VelocityKick.class, (server, packet) -> {
    // Ignore if the player is not on the proxy
    final ConnectedPlayer player = (ConnectedPlayer)  server.getPlayer(packet.getUniqueId()).orElse(null);
    if (player == null) return;

    Component component = packet.deserialize();
    if (component == null) {
      component = Component.text("You have been kicked from the proxy.", NamedTextColor.RED);
    }

    // Disconnect (kick) the player
    player.disconnect0(component, true);
  }),

  /**
   * Handles the {@link VelocityQueueTransfer} packet by queuing the player to the specified queue.
   */
  VELOCITY_QUEUE(VelocityQueueTransfer.class, (server, packet) -> {
    // Ignore if the player is not on the proxy
    final Player player = server.getPlayer(packet.getPayload()).orElse(null);
    if (player == null) {
      return;
    }

    final Queue queue = server.getQueueManager().getQueueCache().getQueue(packet.getQueueName());
    final QueuePlayer queuePlayer = queue.getQueuePlayer(packet.getPayload());
    if (queuePlayer == null) {
      return;
    }

    queuePlayer.handleTransfer();
  })
  ;

  private final RouteRegistration<? extends RedisPacket> routeRegistration;

  <T extends RedisPacket> RouteRegistry(Class<T> packetClass, @NotNull BiConsumer<VelocityServer, T> route) {
    this.routeRegistration = new ConsumerRouteRegistration<>(packetClass,
            packet -> route.accept(VelocityRedis.INSTANCE.getServer(), packet));
  }

  public RouteRegistration<? extends RedisPacket> getRouteRegistration() {
    return routeRegistration;
  }
}
