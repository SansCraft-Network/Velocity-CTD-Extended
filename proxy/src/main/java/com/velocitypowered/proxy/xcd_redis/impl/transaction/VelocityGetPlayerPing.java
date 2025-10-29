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
public final class VelocityGetPlayerPing extends VelocityTransaction<StringPacket, ComponentPacket> {

  public VelocityGetPlayerPing(@NotNull CommandSource source, @NotNull String username) {
    super(new StringPacket(username), source, "xcd_redis.command.ping.timeout");

    // Send the ping result to the command source
    this.onComplete(packet -> PacketBehaviour.SEND_COMPONENT.behave(source, packet));
  }
}
