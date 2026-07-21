/*
 * Copyright (C) 2018-2026 Velocity-CTD Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocityctd.api.server;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;

/**
 * Receives lifecycle callbacks for players using a virtual server.
 *
 * <p>Packet and interaction callbacks will be added with the virtual world runtime. Keeping the
 * initial contract lifecycle-only avoids exposing proxy protocol implementation classes.</p>
 */
public interface VirtualServerHandler {

  /**
   * Invoked after a player has entered the virtual server.
   *
   * @param player the player
   * @param connection the virtual connection
   */
  default void onConnect(Player player, VirtualConnection connection) {
  }

  /**
   * Invoked when a player leaves the virtual server.
   *
   * @param player the player
   * @param connection the virtual connection
   */
  default void onDisconnect(Player player, VirtualConnection connection) {
  }

  /**
   * Handles a chat message sent in the virtual server.
   *
   * @param player the player
   * @param connection the virtual connection
   * @param message the message
   */
  default void onChat(Player player, VirtualConnection connection, String message) {
  }

  /**
   * Handles a command sent in the virtual server. The command does not include a leading slash.
   *
   * @param player the player
   * @param connection the virtual connection
   * @param command the command
   * @return {@code true} to consume the command, or {@code false} to run it through Velocity
   */
  default boolean onCommand(Player player, VirtualConnection connection, String command) {
    return false;
  }

  /**
   * Handles a player movement update.
   *
   * @param player the player
   * @param connection the virtual connection
   * @param position the latest position
   */
  default void onMove(Player player, VirtualConnection connection,
      VirtualPosition position) {
  }

  /**
   * Handles a plugin message sent by the client.
   *
   * @param player the player
   * @param connection the virtual connection
   * @param identifier the channel
   * @param data a copy of the payload
   */
  default void onPluginMessage(Player player, VirtualConnection connection,
      ChannelIdentifier identifier, byte[] data) {
  }
}