/*
 * Copyright (C) 2026 Velocity-CTD Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocityctd.api.server;

import com.velocitypowered.api.network.ProtocolVersion;
import net.kyori.adventure.text.Component;

/**
 * Represents a virtual player connection, allowing packets to be sent and custom packet
 * handling to be defined.
 */
public interface VirtualConnection {

  /**
   * Sends a packet to the player.
   *
   * @param packet the packet to send (typically an instance of MinecraftPacket)
   */
  void sendPacket(Object packet);

  /**
   * Disconnects the player from the proxy with a reason.
   *
   * @param reason the reason for disconnection
   */
  void disconnect(Component reason);

  /**
   * Gets the protocol version used by the player.
   *
   * @return the protocol version
   */
  ProtocolVersion getProtocolVersion();

  /**
   * Sets the packet handler for client-to-proxy packets.
   *
   * @param handler the packet handler
   */
  void setPacketHandler(VirtualPacketHandler handler);
}
