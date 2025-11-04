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

package com.velocitypowered.proxy.queue.cache;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.queue.Queue;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Represents a cache that stores the queue data
 *
 * @author Elmar Blume - 03/04/2025
 */
public sealed interface QueueCache permits MemoryQueueCache, RedisQueueCache {

  Queue getQueue(@NotNull String serverName);

  Queue getQueue(@NotNull Player player);

  Collection<Queue> getQueues();

  void updateQueue(@NotNull Queue queue);
}
