package com.velocitypowered.proxy.xcd_redis.packet.typed;

import com.velocitypowered.proxy.xcd_redis.packet.GenericPacket;

/**
 * @author Elmar Blume - 12/05/2025
 */
public class LongPacket extends GenericPacket<Long> {
  public LongPacket(long payload) {
    super(payload);
  }
}
