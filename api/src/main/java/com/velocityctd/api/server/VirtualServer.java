/*
 * Copyright (C) 2018-2026 Velocity-CTD Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocityctd.api.server;

import com.velocitypowered.api.proxy.server.RegisteredServer;

/**
 * A lightweight server whose world and sessions are managed by the proxy.
 */
public interface VirtualServer extends RegisteredServer {

  /**
   * Returns the immutable definition used to create this server.
   *
   * @return the definition
   */
  VirtualServerDefinition getDefinition();
}