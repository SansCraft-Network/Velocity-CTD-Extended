/*
 * Copyright (C) 2026 Velocity-CTD Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocityctd.api.server;

import com.velocitypowered.api.proxy.Player;

/**
 * Handles connections and disconnections to a virtual server.
 */
public interface VirtualServerHandler {

  /**
   * Called when a player successfully connects to this virtual server.
   *
   * @param player     the player who connected
   * @param connection the virtual connection instance for sending/handling packets
   */
  void onConnect(Player player, VirtualConnection connection);

  /**
   * Called when a player disconnects from this virtual server.
   *
   * @param player the player who disconnected
   */
  void onDisconnect(Player player);
}
