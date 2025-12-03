/*
 * Copyright (C) 2018-2025 Velocity Contributors
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

import com.velocitypowered.proxy.queue.AbstractQueue;

/**
 * Represents the state of a {@link AbstractQueue}.
 */
public enum QueueState {

  /**
   * Represents a state in which the queue is not active and does not process elements.
   */
  INACTIVE,

  /**
   * Represents the operational state of the queue indicating it is currently active
   * and processing tasks or requests.
   */
  ACTIVE,

  /**
   * Indicates that the queue is in a full state, meaning it cannot accept any additional elements.
   */
  FULL,

  /**
   * Indicates that the queue is in a paused state.
   * In this state, the queue temporarily halts its operations
   * and does not process new elements until it is resumed.
   */
  PAUSED
}
