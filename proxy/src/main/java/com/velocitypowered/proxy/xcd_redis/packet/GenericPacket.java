package com.velocitypowered.proxy.xcd_redis.packet;

/**
 * @author Elmar Blume - 08/05/2025
 */
public non-sealed class GenericPacket<T> extends AbstractRedisPacket {

  protected final T payload;

  /**
   * Constructs a new {@link GenericPacket}
   *
   * @param payload the payload of the packet
   */
  public GenericPacket(T payload) {
    super();
    this.payload = payload;
  }

  /**
   * Get the payload of the packet
   *
   * @return the payload of the packet
   */
  public T getPayload() {
    return payload;
  }

}
