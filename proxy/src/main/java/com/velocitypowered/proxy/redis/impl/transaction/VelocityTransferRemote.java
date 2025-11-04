package com.velocitypowered.proxy.redis.impl.transaction;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.redis.impl.PacketBehaviour;
import com.velocitypowered.proxy.redis.impl.packet.VelocityRemote;
import com.velocitypowered.proxy.redis.packet.annotation.OneWayPacket;
import com.velocitypowered.proxy.redis.packet.typed.ComponentPacket;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @author Elmar Blume - 09/05/2025
 */
@OneWayPacket
public final class VelocityTransferRemote extends VelocityTransaction<VelocityRemote, ComponentPacket> {

  public VelocityTransferRemote(@NotNull CommandSource source, UUID uniqueId, String proxyId, String ip, int port) {
    super(new VelocityRemote(uniqueId, proxyId, ip, port), source, "xcd_redis.command.transfer.timeout");

    // Send the result to the command source
    this.onComplete(packet -> PacketBehaviour.SEND_COMPONENT.behave(source, packet));
  }
}
