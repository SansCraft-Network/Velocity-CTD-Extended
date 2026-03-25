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

package com.velocityctd.proxy.connection.profile;

import static com.google.common.net.UrlEscapers.urlFormParameterEscaper;
import static com.velocitypowered.proxy.VelocityServer.GENERAL_GSON;

import com.google.common.base.Stopwatch;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GameProfileFetcher {

  private static final Logger LOGGER = LogManager.getLogger(GameProfileFetcher.class);

  /**
   * The base URL used to verify that a player has joined using Mojang's session server.
   */
  private static final String HASJOINED_BASE_URL =
      System.getProperty("mojang.sessionserver", "https://sessionserver.mojang.com/session/minecraft/hasJoined");

  /**
   * Used when {@link com.velocitypowered.proxy.config.VelocityConfiguration#shouldPreventClientProxyConnections()} is {@code false}.
   */
  private static final String HASJOINED_NO_IP_URL = HASJOINED_BASE_URL.concat("?username=%s&serverId=%s");

  /**
   * Used when {@link com.velocitypowered.proxy.config.VelocityConfiguration#shouldPreventClientProxyConnections()} is {@code true}.
   */
  private static final String HASJOINED_WITH_IP_URL = HASJOINED_BASE_URL.concat("?username=%s&serverId=%s&ip=%s");

  private final VelocityServer server;
  private final HttpClient httpClient;

  public GameProfileFetcher(VelocityServer server) {
    this.server = server;

    httpClient = server.createHttpClient();
  }

  public CompletableFuture<GameProfileResponse> fetchProfile(String playerIp, String username, String serverId) {
    String url;
    if (server.getConfiguration().shouldPreventClientProxyConnections()) {
      url = String.format(HASJOINED_WITH_IP_URL,
          urlFormParameterEscaper().escape(username),
          urlFormParameterEscaper().escape(serverId),
          urlFormParameterEscaper().escape(playerIp));
    } else {
      url = String.format(HASJOINED_NO_IP_URL,
          urlFormParameterEscaper().escape(username),
          urlFormParameterEscaper().escape(serverId));
    }

    HttpRequest httpRequest = HttpRequest.newBuilder()
        .setHeader("User-Agent",
            server.getVersion().getName() + "/" + server.getVersion().getVersion())
        .uri(URI.create(url))
        .build();

    Stopwatch stopwatch = Stopwatch.createStarted(); // For debug logging

    return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
        .handle((response, throwable) -> {
          if (throwable != null) {
            LOGGER.error("Unable to authenticate player", throwable);
            return GameProfileResponse.error(GameProfileResponse.Status.ERROR_AUTH_DOWN);
          }

          stopwatch.stop();
          LOGGER.debug("Fetched game profile in {}.", stopwatch);

          if (response.statusCode() == 200) {
            GameProfile profile = GENERAL_GSON.fromJson(response.body(), GameProfile.class);
            return GameProfileResponse.success(profile);
          } else if (response.statusCode() == 204) {
            // An offline-mode user logged onto this online-mode proxy.
            return GameProfileResponse.error(GameProfileResponse.Status.ERROR_OFFLINE_USER);
          } else {
            // Something else went wrong
            LOGGER.error(
                "Got an unexpected error code {} whilst contacting Mojang to log in {} ({})",
                response.statusCode(), username, playerIp);
            return GameProfileResponse.error(GameProfileResponse.Status.ERROR_AUTH_DOWN);
          }
        });
  }
}
