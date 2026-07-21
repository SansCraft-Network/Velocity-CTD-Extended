/*
 * Copyright (C) 2018-2026 Velocity-CTD Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocityctd.api.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VirtualServerDefinitionTest {

  @Test
  void rejectsBlankNames() {
    assertThrows(IllegalArgumentException.class,
        () -> VirtualServerDefinition.builder(" "));
  }

  @Test
  void buildsSafeDefaults() {
    VirtualServerDefinition definition =
        VirtualServerDefinition.builder("holding").build();

    assertEquals("holding", definition.getName());
    assertEquals("minecraft:overworld", definition.getDimension().asString());
    assertEquals(64, definition.getSpawnY());
    assertEquals(VirtualGameMode.ADVENTURE, definition.getGameMode());
    assertEquals(6000, definition.getWorldTime());
  }
}