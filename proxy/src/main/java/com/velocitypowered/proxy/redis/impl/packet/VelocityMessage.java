package com.velocitypowered.proxy.redis.impl.packet;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.redis.impl.model.EncodedCommandSource;
import com.velocitypowered.proxy.redis.packet.annotation.OneWayPacket;
import com.velocitypowered.proxy.redis.packet.typed.ComponentPacket;
import net.kyori.adventure.text.Component;

import java.util.UUID;

/**
 * @author Elmar Blume - 09/05/2025
 */
@OneWayPacket
public final class VelocityMessage extends ComponentPacket {

  private EncodedCommandSource commandSource = null;
  private UUID playerUniqueId = null;

  public VelocityMessage(EncodedCommandSource commandSource, Component component) {
    super(component);
    this.commandSource = commandSource;
  }

  public VelocityMessage(UUID playerUniqueId, Component component) {
    super(component);
    this.playerUniqueId = playerUniqueId;
  }

  public void sendMessage(final VelocityServer server) {
    final Component component = this.deserialize();
    if (component == null) {
      return;
    }

    if (this.playerUniqueId != null) {
      server.getPlayer(this.playerUniqueId).ifPresent(player -> {
        player.sendMessage(component);
      });
    } else if (this.commandSource != null) {
      this.commandSource.sendMessage(server, component);
    }
  }
}
