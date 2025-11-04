package com.velocitypowered.proxy.redis.packet.typed;

import com.velocitypowered.proxy.redis.packet.GenericPacket;

import java.util.UUID;

/**
 * @author Elmar Blume - 12/05/2025
 */
public class UUIDPacket extends GenericPacket<UUID> {
  public UUIDPacket(UUID payload) {
    super(payload);
  }
}
