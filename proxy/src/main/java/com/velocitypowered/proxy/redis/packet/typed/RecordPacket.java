package com.velocitypowered.proxy.redis.packet.typed;

import com.velocitypowered.proxy.redis.packet.GenericPacket;

/**
 * @author Elmar Blume - 15/05/2025
 */
public class RecordPacket<T extends Record> extends GenericPacket<T> {

  public RecordPacket(T payload) {
    super(payload);
  }

}
