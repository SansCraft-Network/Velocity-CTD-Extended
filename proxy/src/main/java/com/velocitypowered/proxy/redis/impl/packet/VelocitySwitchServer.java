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

import com.velocitypowered.proxy.redis.packet.annotation.OneWayPacket;
import com.velocitypowered.proxy.redis.packet.typed.StringPacket;

/**
 * Represents a packet that sends a player to a specific server.
 *
 * @author Elmar Blume - 20/06/2025
 */
@OneWayPacket
public final class VelocitySwitchServer extends StringPacket {

  private final String username;
  private final String serverName;

  /**
   * Constructs a new {@link VelocitySwitchServer} packet.
   *
   * @param username the username of the player to switch to the server of.
   * @param serverName the name of the server to switch to.
   */
  public VelocitySwitchServer(String username, String serverName) {
    super(username);

    this.username = username;
    this.serverName = serverName;
  }

  /**
   * Gets the username of the player to switch to the server of.
   *
   * @return the username of the player to switch to the server of.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Gets the name of the server to switch to.
   *
   * @return the name of the server to switch to.
   */
  public String getServerName() {
    return serverName;
  }
}
