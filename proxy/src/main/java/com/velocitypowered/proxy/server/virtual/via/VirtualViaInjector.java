/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.velocitypowered.proxy.server.virtual.via;

import com.velocitypowered.proxy.network.Connections;
import com.viaversion.viaversion.platform.NoopInjector;

public class VirtualViaInjector extends NoopInjector {

  @Override
  public String getEncoderName() {
    return Connections.VIRTUAL_VIA_CODEC;
  }

  @Override
  public String getDecoderName() {
    return Connections.VIRTUAL_VIA_CODEC;
  }
}
