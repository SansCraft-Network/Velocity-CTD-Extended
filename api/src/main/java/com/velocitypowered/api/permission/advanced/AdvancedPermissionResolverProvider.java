/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.permission.advanced;

import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionSubject;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides {@link AdvancedPermissionResolver}s for {@link PermissionSubject}s.
 */
public interface AdvancedPermissionResolverProvider {

  /**
   * Should perform a check to see if this {@link AdvancedPermissionResolverProvider} is available or not.
   * If this method returns {@code false}, subsequent calls to the provider's functions may
   * result in errors or undefined behavior.
   *
   * @return {@code true} if this {@link AdvancedPermissionResolverProvider} is available, {@code false} if not
   */
  boolean isAvailable();

  /**
   * Creates a {@link AdvancedPermissionResolver} for the subject with an optional {@link PermissionFunction} delegate.
   *
   * @param subject the subject
   * @param delegate the optional delegate to use
   * @return the permission resolver
   */
  @Nullable AdvancedPermissionResolver createResolver(PermissionSubject subject, @Nullable PermissionFunction delegate);

  /**
   * Creates a {@link AdvancedPermissionResolver} for the subject without a delegate.
   *
   * @param subject the subject
   * @return the permission resolver
   */
  default @Nullable AdvancedPermissionResolver createResolver(PermissionSubject subject) {
    return createResolver(subject, null);
  }
}
