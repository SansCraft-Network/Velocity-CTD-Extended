package com.velocitypowered.proxy.redis.packet.typed;

import com.velocitypowered.proxy.redis.packet.GenericPacket;

/**
 * @author Elmar Blume - 12/05/2025
 */
public class BooleanPacket extends GenericPacket<Boolean>{
  public BooleanPacket(Boolean payload) {
    super(payload);
  }
}
