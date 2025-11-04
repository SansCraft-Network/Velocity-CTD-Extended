package com.velocitypowered.proxy.redis.impl.transaction;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.redis.packet.RedisPacket;
import com.velocitypowered.proxy.redis.transaction.Transaction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elmar Blume - 02/10/2025
 */
public abstract class VelocityTransaction<T extends RedisPacket, R extends RedisPacket> extends Transaction<T, R> {

  public VelocityTransaction(@NotNull T sentPacket, @Nullable CommandSource source, @Nullable String timeoutTranslationKey) {
    super(sentPacket);

    // Set the default timeout behavior to send a message to the command source
    if (source != null && timeoutTranslationKey != null)
      this.onTimeout(t -> source.sendMessage(Component.text(timeoutTranslationKey, NamedTextColor.RED)));
  }

  public VelocityTransaction(@NotNull T sentPacket) {
    this(sentPacket, null, null);
  }
}
