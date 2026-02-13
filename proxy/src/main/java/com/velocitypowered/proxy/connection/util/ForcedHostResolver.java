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

package com.velocitypowered.proxy.connection.util;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.proxy.VelocityServer;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Contract;

/**
 * Resolves the configured backend server order to try for an inbound connection based on its
 * virtual host and Velocity's forced-host rules.
 */
public class ForcedHostResolver {

  private ForcedHostResolver() {
  }

  /**
   * Returns a lowercase (Locale.ROOT) host string for the given address, or {@code null} if {@code address} is {@code null}.
   *
   * @param address the socket address to normalize (may be {@code null})
   * @return the normalized host string, or {@code null} if {@code address} is {@code null}
   */
  @Contract("null -> null; !null -> !null")
  public static @Nullable String normalizeHostString(@Nullable InetSocketAddress address) {
    if (address == null) {
      return null;
    }

    return address.getHostString().toLowerCase(Locale.ROOT);
  }

  /**
   * Returns the forced-host server list for the connection's virtual host (exact match, then {@code *.<suffix>} match),
   * or falls back to the configured attempt-connection order if no rule matches.
   *
   * @param velocityServer the Velocity server instance providing configuration access
   * @param connection the inbound connection whose virtual host is used for forced-host resolution
   * @return an unmodifiable list of backend server names to try, in order
   */
  public static List<String> resolveServersToTry(
      VelocityServer velocityServer,
      InboundConnection connection
  ) {
    String virtualHost = connection.getVirtualHost()
        .map(ForcedHostResolver::normalizeHostString)
        .orElse(null);
    if (virtualHost != null) {
      List<String> forcedHosts = velocityServer.getConfiguration()
          .getForcedHosts()
          .getOrDefault(virtualHost, emptyList());

      // Check for wildcard ("*.server.com" matches "anything.server.com")
      if (forcedHosts.isEmpty()) {
        for (Map.Entry<String, List<String>> entry : velocityServer.getConfiguration().getForcedHosts().entrySet()) {
          String pattern = entry.getKey().toLowerCase(Locale.ROOT);
          if (pattern.startsWith("*.") && virtualHost.endsWith(pattern.substring(1))) {
            forcedHosts = entry.getValue();
            break;
          }
        }
      }

      if (!forcedHosts.isEmpty()) {
        return unmodifiableList(forcedHosts);
      }
    }

    return unmodifiableList(velocityServer.getConfiguration().getAttemptConnectionOrder());
  }
}
