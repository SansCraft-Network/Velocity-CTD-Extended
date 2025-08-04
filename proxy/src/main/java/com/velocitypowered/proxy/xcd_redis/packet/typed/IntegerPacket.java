package com.velocitypowered.proxy.xcd_redis.packet.typed;

import com.velocitypowered.proxy.xcd_redis.packet.GenericPacket;

/**
 * @author Elmar Blume - 12/05/2025
 */
public class IntegerPacket extends GenericPacket<Integer> {
  public IntegerPacket(Integer payload) {
    super(payload);
  }
}
