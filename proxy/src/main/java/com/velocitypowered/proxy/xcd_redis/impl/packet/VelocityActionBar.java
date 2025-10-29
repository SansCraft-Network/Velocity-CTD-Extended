package com.velocitypowered.proxy.xcd_redis.impl.packet;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.xcd_redis.impl.model.EncodedCommandSource;
import com.velocitypowered.proxy.xcd_redis.packet.annotation.OneWayPacket;
import com.velocitypowered.proxy.xcd_redis.packet.typed.ComponentPacket;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @author Elmar Blume - 09/05/2025
 */
@OneWayPacket
public final class VelocityActionBar extends ComponentPacket {

  private final UUID uniqueId;

  public VelocityActionBar(final @NotNull Player player, final Component component) {
    super(component);
    this.uniqueId = player.getUniqueId();
  }

  public UUID getUniqueId() {
    return uniqueId;
  }
}
