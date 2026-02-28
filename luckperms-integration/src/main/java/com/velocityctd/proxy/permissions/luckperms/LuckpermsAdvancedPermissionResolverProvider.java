/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

package com.velocityctd.proxy.permissions.luckperms;

import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.advanced.AdvancedPermissionResolver;
import com.velocitypowered.api.permission.advanced.AdvancedPermissionResolverProvider;
import com.velocitypowered.api.proxy.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A provider class to generate {@link AdvancedPermissionResolver} classes backed by
 * {@link LuckpermsAdvancedPermissionResolver}. This provider is only available if
 * {@code net.luckperms.api.LuckPermsProvider} is available on the classpath, effectively checking
 * if we have the LuckPerms plugin installed and available for the advanced permission checks.
 */
public class LuckpermsAdvancedPermissionResolverProvider implements AdvancedPermissionResolverProvider {

  @Override
  public boolean isAvailable() {
    try {
      Class.forName("net.luckperms.api.LuckPermsProvider", false, getClass().getClassLoader());
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public @Nullable AdvancedPermissionResolver createResolver(PermissionSubject subject, @Nullable PermissionFunction delegate) {
    if (!(subject instanceof Player player) || delegate == null) {
      return null;
    }

    return new LuckpermsAdvancedPermissionResolver(player, delegate);
  }
}
