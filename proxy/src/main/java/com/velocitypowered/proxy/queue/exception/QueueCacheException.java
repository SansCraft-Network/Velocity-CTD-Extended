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

package com.velocitypowered.proxy.queue.exception;

/**
 * Represents an exception thrown when a queue caching operation fails.
 *
 * @author Elmar Blume - 03/04/2025
 */
public final class QueueCacheException extends RuntimeException {

  /**
   * Constructs a new {@link QueueCacheException}.
   *
   * @param serverName the server name
   */
  public QueueCacheException(String serverName) {
    super("Attempted to fetch queue for invalid server: '%s'".formatted(serverName));
  }

}
