/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.permission;

import com.velocitypowered.api.permission.advanced.AdvancedPermissionResolver;
import java.util.Map;
import net.kyori.adventure.permission.PermissionChecker;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents an object that has a set of queryable permissions.
 */
public interface PermissionSubject {

  /**
   * Determines whether the subject has a particular permission.
   *
   * @param permission the permission to check for
   * @return whether the subject has the permission
   */
  default boolean hasPermission(String permission) {
    return getPermissionValue(permission).asBoolean();
  }

  /**
   * Gets the subjects setting for a particular permission.
   * Used as a delegate for {@link PermissionFunction#getPermissionValue(String)}
   *
   * @param permission the permission
   * @return the value the permission is set to
   */
  @NonNull
  Tristate getPermissionValue(String permission);

  /**
   * Gets the subjects permission map.
   * Used as a delegate for {@link AdvancedPermissionResolver#getPermissionMap()}
   *
   * @return the permission map
   */
  @NonNull
  @Unmodifiable
  Map<String, Boolean> getPermissionMap();

  /**
   * Gets the permission checker for the subject.
   *
   * @return subject's permission checker
   */
  default PermissionChecker getPermissionChecker() {
    return permission -> getPermissionValue(permission).toAdventureTriState();
  }
}
