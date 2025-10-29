package com.velocitypowered.proxy.xcd_redis.impl.transaction;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.xcd_redis.impl.PacketBehaviour;
import com.velocitypowered.proxy.xcd_redis.packet.typed.ComponentPacket;
import com.velocitypowered.proxy.xcd_redis.packet.typed.StringPacket;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elmar Blume - 02/10/2025
 */
public final class VelocityReload extends VelocityTransaction<StringPacket, ComponentPacket> {

  public VelocityReload(@NotNull CommandSource source, @NotNull String proxyId) {
    super(new StringPacket(proxyId), source, "xcd_redis.command.reload.timeout");

    // Send the uptime result to the command source
    this.onComplete(packet -> PacketBehaviour.SEND_COMPONENT.behave(source, packet));
  }
}
