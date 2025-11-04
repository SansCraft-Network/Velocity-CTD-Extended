/*
 * Copyright (C) 2025 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.redis.impl.packet;

import com.velocitypowered.proxy.redis.packet.typed.UUIDPacket;
import java.util.UUID;

/**
 * @author Elmar Blume - 06/10/2025
 */
public final class VelocityRemote extends UUIDPacket {

  private final String proxyId;
  private final String ip;
  private final int port;

  public VelocityRemote(UUID payload, String proxyId, String ip, int port) {
    super(payload);

    this.proxyId = proxyId;
    this.ip = ip;
    this.port = port;
  }

  public String getProxyId() {
    return proxyId;
  }

  public String getIp() {
    return ip;
  }

  public int getPort() {
    return port;
  }
}
