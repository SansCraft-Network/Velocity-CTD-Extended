/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.velocitypowered.proxy.server.virtual.registry;

public enum VirtualDimension {

  OVERWORLD("minecraft:overworld", 0, 0, 28, true),
  NETHER("minecraft:the_nether", -1, 1, 16, false),
  THE_END("minecraft:the_end", 1, 2, 16, false);

  private final String key;
  private final int legacyId;
  private final int modernId;
  private final int maxSections;
  private final boolean hasLegacySkyLight;

  VirtualDimension(String key, int legacyId, int modernId, int maxSections, boolean hasLegacySkyLight) {
    this.key = key;
    this.legacyId = legacyId;
    this.modernId = modernId;
    this.maxSections = maxSections;
    this.hasLegacySkyLight = hasLegacySkyLight;
  }

  public String getKey() {
    return key;
  }

  public int getLegacyId() {
    return legacyId;
  }

  public int getModernId() {
    return modernId;
  }

  public int getMaxSections() {
    return maxSections;
  }

  public boolean hasLegacySkyLight() {
    return hasLegacySkyLight;
  }
}
