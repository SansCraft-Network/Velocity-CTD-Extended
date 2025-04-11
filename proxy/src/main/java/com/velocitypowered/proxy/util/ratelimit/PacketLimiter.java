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

package com.velocitypowered.proxy.util.ratelimit;

/**
 * Class to limit the number of packets and data a player can send per second.
 */
public final class PacketLimiter {
  private final int limit;
  private final int dataLimit;

  private int counter;
  private int dataCounter;
  private long nextSecond;

  /**
    * Creates a new packet limiter.
    *
    * @param limit max number of packets allowed per second
    * @param dataLimit max amount of data allowed per second
    */
  public PacketLimiter(final int limit, final int dataLimit) {
    this.limit = limit;
    this.dataLimit = dataLimit;
  }

  /**
    * Counts the received packet amount and size.
    *
    * @param size size of the packet
    *
    * @return return false if the player should be kicked
    */
  public boolean incrementAndCheck(final int size) {
    counter++;
    dataCounter += size;

    if ((limit > 0 && counter > limit) || (dataLimit > 0 && dataCounter > dataLimit)) {
      long now = System.currentTimeMillis();
      if (nextSecond > now) {
        return false;
      }

      nextSecond = now + 1000;
      counter = 0;
      dataCounter = 0;
    }

    return true;
  }

  public int getDataCounter() {
    return dataCounter;
  }

  public int getCounter() {
    return counter;
  }
}
