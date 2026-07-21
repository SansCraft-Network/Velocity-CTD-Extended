/*
 * Copyright (C) 2018-2026 Velocity-CTD Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocityctd.api.server;

import static java.util.Objects.requireNonNull;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/**
 * Immutable configuration for a proxy-managed virtual server.
 */
public final class VirtualServerDefinition {

  private final String name;
  private final Key dimension;
  private final double spawnX;
  private final double spawnY;
  private final double spawnZ;
  private final float spawnYaw;
  private final float spawnPitch;
  private final VirtualGameMode gameMode;
  private final long worldTime;
  private final Component description;
  private final VirtualServerHandler handler;

  private VirtualServerDefinition(Builder builder) {
    this.name = builder.name;
    this.dimension = builder.dimension;
    this.spawnX = builder.spawnX;
    this.spawnY = builder.spawnY;
    this.spawnZ = builder.spawnZ;
    this.spawnYaw = builder.spawnYaw;
    this.spawnPitch = builder.spawnPitch;
    this.gameMode = builder.gameMode;
    this.worldTime = builder.worldTime;
    this.description = builder.description;
    this.handler = builder.handler;
  }

  public static Builder builder(String name) {
    return new Builder(name);
  }

  public String getName() {
    return name;
  }

  public Key getDimension() {
    return dimension;
  }

  public double getSpawnX() {
    return spawnX;
  }

  public double getSpawnY() {
    return spawnY;
  }

  public double getSpawnZ() {
    return spawnZ;
  }

  public float getSpawnYaw() {
    return spawnYaw;
  }

  public float getSpawnPitch() {
    return spawnPitch;
  }

  public VirtualGameMode getGameMode() {
    return gameMode;
  }

  public long getWorldTime() {
    return worldTime;
  }

  public Component getDescription() {
    return description;
  }

  public VirtualServerHandler getHandler() {
    return handler;
  }

  /**
   * Builds a virtual server definition.
   */
  public static final class Builder {

    private final String name;
    private Key dimension = Key.key("minecraft", "overworld");
    private double spawnX;
    private double spawnY = 64;
    private double spawnZ;
    private float spawnYaw;
    private float spawnPitch;
    private VirtualGameMode gameMode = VirtualGameMode.ADVENTURE;
    private long worldTime = 6000;
    private Component description = Component.text("Velocity-CTD virtual server");
    private VirtualServerHandler handler = new VirtualServerHandler() {
    };

    private Builder(String name) {
      requireNonNull(name, "name");
      if (name.isBlank()) {
        throw new IllegalArgumentException("name must not be blank");
      }
      this.name = name;
    }

    public Builder dimension(Key dimension) {
      this.dimension = requireNonNull(dimension, "dimension");
      return this;
    }

    /**
     * Sets the initial player position.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @param yaw the yaw
     * @param pitch the pitch
     * @return this builder
     */
    public Builder spawn(double x, double y, double z, float yaw, float pitch) {
      this.spawnX = x;
      this.spawnY = y;
      this.spawnZ = z;
      this.spawnYaw = yaw;
      this.spawnPitch = pitch;
      return this;
    }

    public Builder gameMode(VirtualGameMode gameMode) {
      this.gameMode = requireNonNull(gameMode, "gameMode");
      return this;
    }

    public Builder worldTime(long worldTime) {
      this.worldTime = worldTime;
      return this;
    }

    public Builder description(Component description) {
      this.description = requireNonNull(description, "description");
      return this;
    }

    public Builder handler(VirtualServerHandler handler) {
      this.handler = requireNonNull(handler, "handler");
      return this;
    }

    public VirtualServerDefinition build() {
      return new VirtualServerDefinition(this);
    }
  }
}