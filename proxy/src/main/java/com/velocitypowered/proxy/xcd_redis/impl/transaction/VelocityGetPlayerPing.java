package com.velocitypowered.proxy.xcd_redis.impl.transaction;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.xcd_redis.impl.PacketBehaviour;
import com.velocitypowered.proxy.xcd_redis.packet.typed.ComponentPacket;
import com.velocitypowered.proxy.xcd_redis.packet.typed.StringPacket;
import com.velocitypowered.proxy.xcd_redis.transaction.Transaction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * @author Elmar Blume - 14/05/2025
 */
public final class VelocityGetPlayerPing extends Transaction<StringPacket, ComponentPacket> {

  public VelocityGetPlayerPing(@NotNull CommandSource source, @NotNull String username) {
    super(new StringPacket(username));

    this.onComplete(packet -> PacketBehaviour.SEND_COMPONENT.behave(packet, source));

    this.onTimeout(packet -> {
      final TextComponent component = Component.text("Unable to get ping for player " + username,
              NamedTextColor.RED);
      source.sendMessage(component);
    });
  }
}
