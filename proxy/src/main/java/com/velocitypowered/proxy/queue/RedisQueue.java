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

package com.velocitypowered.proxy.queue;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.queue.redis.depot.QueueEntry;
import com.velocitypowered.proxy.redis.impl.packet.VelocityMessage;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

/**
 * Represents the redis implementation of {@link Queue}.
 *
 * @author Elmar Blume - 29/10/2025
 */
public final class RedisQueue extends AbstractQueue {

  /**
   * Constructs a new {@link RedisQueue}.
   *
   * @param server the proxy instance
   * @param backendInstance the backend instance server
   */
  public RedisQueue(VelocityServer server, VelocityRegisteredServer backendInstance) {
    super(server, backendInstance);
  }

  /**
   * Constructs a new {@link RedisQueue}.
   *
   * @param server the proxy instance
   * @param backendInstance the backend instance server
   * @param queueEntry the queue entry
   */
  public RedisQueue(VelocityServer server, VelocityRegisteredServer backendInstance, QueueEntry queueEntry) {
    super(server, backendInstance, queueEntry);
  }

  @Override
  protected void notifyMaxRetriesReached(UUID uniqueId) {
    final TranslatableComponent component = Component.translatable("velocity.queue.error.max-send-retries-reached")
            .arguments(Component.text(this.getName()), Component.text(this.server.getConfiguration().getQueue().getMaxSendRetries()));

    new VelocityMessage(uniqueId, component)
            .publish();
  }
}
