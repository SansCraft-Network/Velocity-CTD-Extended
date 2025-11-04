package com.velocitypowered.proxy.redis.packet.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link com.velocitypowered.proxy.redis.packet.RedisPacket} as a one way packet. Which
 * means that the packet is not expected to be answered by the sender.
 *
 * @author Elmar Blume - 08/05/2025
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OneWayPacket {
}
