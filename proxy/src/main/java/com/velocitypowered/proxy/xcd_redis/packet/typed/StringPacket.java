package com.velocitypowered.proxy.xcd_redis.packet.typed;

import com.velocitypowered.proxy.xcd_redis.packet.GenericPacket;

/**
 * @author Elmar Blume - 12/05/2025
 */
public class StringPacket extends GenericPacket<String> {
  public StringPacket(String payload) {
    super(payload);
  }
}
