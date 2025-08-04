package com.velocitypowered.proxy.xcd_redis.impl;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.xcd_redis.VelocityRedis;
import com.velocitypowered.proxy.xcd_redis.impl.packet.VelocityAlert;
import com.velocitypowered.proxy.xcd_redis.packet.RedisPacket;
import com.velocitypowered.proxy.xcd_redis.registration.RouteRegistration;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * @author Elmar Blume - 17/05/2025
 */
public enum RouteRegistry {

  VELOCITY_ALERT((server) -> RouteRegistration.consumer(VelocityAlert.class,
          packet -> PacketBehaviour.SEND_COMPONENT.behave(packet, server))),
  ;

  private final RouteRegistration<? extends RedisPacket> routeRegistration;

  RouteRegistry(@NotNull Function<VelocityServer, RouteRegistration<? extends RedisPacket>> route) {
    this.routeRegistration = route.apply(VelocityRedis.INSTANCE.getServer());
  }

  public RouteRegistration<? extends RedisPacket> getRouteRegistration() {
    return routeRegistration;
  }
}
