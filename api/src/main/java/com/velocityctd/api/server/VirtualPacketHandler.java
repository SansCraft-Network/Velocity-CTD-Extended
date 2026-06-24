/*
 * Copyright (C) 2026 Velocity-CTD Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocityctd.api.server;

/**
 * Handles incoming packets sent from the client to the proxy while connected to a virtual server.
 */
public interface VirtualPacketHandler {

  /**
   * Handles an incoming client packet.
   *
   * @param packet the packet (can be a MinecraftPacket or direct ByteBuf depending on registration)
   * @return true if the packet was handled and should not be processed further by Velocity
   */
  boolean handlePacket(Object packet);
}
