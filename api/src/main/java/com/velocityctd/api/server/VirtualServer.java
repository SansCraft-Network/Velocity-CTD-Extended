/*
 * Copyright (C) 2026 Velocity-CTD Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocityctd.api.server;

import com.velocitypowered.api.proxy.server.RegisteredServer;

/**
 * Represents a virtual server registered in the proxy.
 * Virtual servers do not connect to a physical Minecraft server socket,
 * but instead delegate player packet handling directly to a custom handler.
 */
public interface VirtualServer extends RegisteredServer {

  /**
   * Gets the handler associated with this virtual server.
   *
   * @return the virtual server handler
   */
  VirtualServerHandler getHandler();
}
