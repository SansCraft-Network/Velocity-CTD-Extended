package com.velocitypowered.proxy.queue;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.queue.redis.depot.QueueEntry;
import com.velocitypowered.proxy.redis.impl.packet.VelocityMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.translation.Argument;

/**
 * @author Elmar Blume - 29/10/2025
 */
public final class RedisQueue extends AbstractQueue {

  public RedisQueue(VelocityServer server, VelocityRegisteredServer backendInstance) {
    super(server, backendInstance);
  }

  public RedisQueue(VelocityServer server, VelocityRegisteredServer backendInstance, QueueEntry queueEntry) {
    super(server, backendInstance, queueEntry);
  }

  @Override
  protected void notifyMaxRetriesReached(Player player) {
    final TranslatableComponent component = Component.translatable("velocity.queue.error.max-send-retries-reached")
            .arguments(Argument.string("server", this.getName()),
                    Argument.numeric("retries", this.server.getConfiguration().getQueue().getMaxSendRetries()));

    new VelocityMessage(player.getUniqueId(), component)
            .publish();
  }
}
