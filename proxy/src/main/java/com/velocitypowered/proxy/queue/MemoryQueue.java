package com.velocitypowered.proxy.queue;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.translation.Argument;

/**
 * @author Elmar Blume - 29/10/2025
 */
public final class MemoryQueue extends AbstractQueue {

  public MemoryQueue(VelocityServer server,  VelocityRegisteredServer backendInstance) {
    super(server, backendInstance);
  }

  @Override
  protected void notifyMaxRetriesReached(Player player) {
    final TranslatableComponent component = Component.translatable("velocity.queue.error.max-send-retries-reached")
            .arguments(Argument.string("server", this.getName()),
                    Argument.numeric("retries", this.server.getConfiguration().getQueue().getMaxSendRetries()));

    player.sendMessage(component);
  }
}
