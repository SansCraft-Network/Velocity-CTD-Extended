/*
 * Copyright (C) 2018-2026 Velocity-CTD Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocityctd.api.server;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;

/**
 * A safe, protocol-independent view of a player's virtual server session.
 */
public interface VirtualConnection {

  /**
   * Returns the connected player.
   *
   * @return the player
   */
  Player getPlayer();

  /**
   * Returns the virtual server.
   *
   * @return the virtual server
   */
  VirtualServer getServer();

  /**
   * Transfers the player using the normal Velocity connection API.
   *
   * @param destination the destination
   * @return the connection result
   */
  default CompletableFuture<ConnectionRequestBuilder.Result> transferTo(
      RegisteredServer destination) {
    return getPlayer().createConnectionRequest(destination).connect();
  }

  /**
   * Sends an Adventure message to the player.
   *
   * @param message the message
   */
  default void sendMessage(Component message) {
    getPlayer().sendMessage(message);
  }

  /**
   * Disconnects the player.
   *
   * @param reason the reason
   */
  default void disconnect(Component reason) {
    getPlayer().disconnect(reason);
  }
}