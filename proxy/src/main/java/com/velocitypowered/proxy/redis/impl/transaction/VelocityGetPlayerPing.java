package com.velocitypowered.proxy.redis.impl.transaction;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.redis.impl.PacketBehaviour;
import com.velocitypowered.proxy.redis.packet.typed.ComponentPacket;
import com.velocitypowered.proxy.redis.packet.typed.StringPacket;
import org.jetbrains.annotations.NotNull;

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
