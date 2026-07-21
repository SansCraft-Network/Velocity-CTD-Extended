/*
 * Copyright (C) 2018-2026 Velocity-CTD Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocityctd.api.server;

/**
 * Immutable position reported by a player in a virtual server.
 *
 * @param x the X coordinate
 * @param y the Y coordinate
 * @param z the Z coordinate
 * @param yaw the yaw
 * @param pitch the pitch
 * @param onGround whether the client reports being on the ground
 */
public record VirtualPosition(double x, double y, double z, float yaw, float pitch,
                              boolean onGround) {
}