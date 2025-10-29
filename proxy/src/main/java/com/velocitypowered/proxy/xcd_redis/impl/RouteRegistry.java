package com.velocitypowered.proxy.xcd_redis.impl;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.xcd_redis.VelocityRedis;
import com.velocitypowered.proxy.xcd_redis.impl.packet.*;
import com.velocitypowered.proxy.xcd_redis.impl.transaction.VelocityTransferRemote;
import com.velocitypowered.proxy.xcd_redis.packet.RedisPacket;
import com.velocitypowered.proxy.xcd_redis.registration.ConsumerRouteRegistration;
import com.velocitypowered.proxy.xcd_redis.registration.RouteRegistration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
    final Component component = packet.deserialize();
    if (component == null) return;

    // Send the message to the target
    packet.getTarget().sendMessage(server, component);
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
