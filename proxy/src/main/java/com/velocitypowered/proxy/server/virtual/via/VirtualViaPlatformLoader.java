/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.velocitypowered.proxy.server.virtual.via;

import com.viaversion.viaversion.api.platform.ViaPlatformLoader;

public class VirtualViaPlatformLoader implements ViaPlatformLoader {

  @Override
  public void load() {
    // Custom providers can be registered here if needed
  }

  @Override
  public void unload() {
  }
}
