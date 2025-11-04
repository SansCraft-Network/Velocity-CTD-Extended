/*
 * Copyright (C) 2025 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
