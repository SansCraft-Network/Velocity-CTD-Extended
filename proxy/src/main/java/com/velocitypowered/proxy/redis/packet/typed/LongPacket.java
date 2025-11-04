package com.velocitypowered.proxy.redis.packet.typed;

import com.velocitypowered.proxy.redis.packet.GenericPacket;

/**
 * @author Elmar Blume - 12/05/2025
 */
public class LongPacket extends GenericPacket<Long> {
  public LongPacket(long payload) {
    super(payload);
  }
}
