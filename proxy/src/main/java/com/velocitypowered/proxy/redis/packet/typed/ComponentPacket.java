package com.velocitypowered.proxy.redis.packet.typed;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Elmar Blume - 13/05/2025
 */
public class ComponentPacket extends StringPacket {
  private static final Logger LOGGER = LoggerFactory.getLogger(ComponentPacket.class);
  private static final GsonComponentSerializer SERIALIZER = GsonComponentSerializer.gson();

  public ComponentPacket(Component component) {
    super(SERIALIZER.serialize(component));
  }

  /**
   * Gets the deserialized component out of this packet
   *
   * @return the deserialized component, or {@code null} if the component was invalid
   */
  public @Nullable Component deserialize() {
    try {
      return SERIALIZER.deserialize(this.payload);
    } catch (Exception exception) {
      LOGGER.warn("Failed to deserialize component from packet payload", exception);
      return null;
    }
  }
}
