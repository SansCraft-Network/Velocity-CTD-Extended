/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

package com.velocitypowered.proxy.queue.model;

/**
 * Enumerates the status the backend server may be in for queuing.
 */
public enum ServerStatus {

  /**
   * Indicates the backend server is currently unreachable or has failed to respond.
   * Players will not be connected or queued to this server.
   */
  OFFLINE,

  /**
   * Indicates the backend server is not yet ready to accept players but is expected to be online soon.
   * Players may remain queued until the server becomes available.
   */
  WAITING,

  /**
   * Indicates the backend server is online and able to accept player connections.
   */
  ONLINE,

  /**
   * Indicates the backend server is online but has reached its maximum player capacity.
   * Players without the full-server bypass permission will not be transferred until a slot opens.
   */
  FULL;

  /**
   * Returns {@code true} if the backend server is in any active state, meaning it is reachable
   * and eligible for player transfers. This covers both {@link #ONLINE} and {@link #FULL}.
   *
   * @return {@code true} for {@code ONLINE} and {@code FULL}, {@code false} otherwise
   */
  public boolean isActive() {
    return this == ONLINE || this == FULL;
  }
}
