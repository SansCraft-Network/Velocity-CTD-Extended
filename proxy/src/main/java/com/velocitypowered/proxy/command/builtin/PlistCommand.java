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

package com.velocitypowered.proxy.command.builtin;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.command.VelocityCommands;
import com.velocitypowered.proxy.redis.impl.depot.PlayerEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.translation.Argument;

/**
 * Implements Velocity-CTD's {@code /plist} command.
 */
public class PlistCommand {

  private static final String SERVER_ARG = "server";

  private static final String PROXY_ARG = "proxy";

  private final VelocityServer server;

  public PlistCommand(final VelocityServer server) {
    this.server = server;
  }

  /**
   * Returns the command instance if enabled, or {@code null} if disabled via configuration.
   *
   * @return the command instance or {@code null} if disabled
   */
  public BrigadierCommand register() {
    if (!server.isRedisEnabled()) {
      return null;
    }

    final LiteralArgumentBuilder<CommandSource> rootNode = BrigadierCommand
        .literalArgumentBuilder("plist")
        .requires(source ->
            source.getPermissionValue("velocity.command.plist") == Tristate.TRUE)
        .executes(this::totalCount);
    final ArgumentCommandNode<CommandSource, String> serverNode = BrigadierCommand
        .requiredArgumentBuilder(PROXY_ARG, StringArgumentType.string())
        .suggests((ctx, builder) -> VelocityCommands.suggestProxy(server, ctx, builder))
        .executes(this::proxyCount)
        .then(
          BrigadierCommand.requiredArgumentBuilder(SERVER_ARG, StringArgumentType.string())
          .suggests((context, builder) -> {
            final String argument = context.getArguments().containsKey(SERVER_ARG)
                ? context.getArgument(SERVER_ARG, String.class)
                : "";

            for (RegisteredServer registeredServer : server.getAllServers()) {
              final String serverName = registeredServer.getServerInfo().getName();
              if (serverName.regionMatches(true, 0, argument, 0, argument.length())) {
                builder.suggest(serverName);
              }
            }

            if ("all".regionMatches(true, 0, argument, 0, argument.length())) {
              builder.suggest("all");
            }

            return builder.buildFuture();
          })
          .executes(this::serverCount)
          .build()
        )
        .build();

    rootNode.then(serverNode);
    return new BrigadierCommand(rootNode);
  }

  private int totalCount(final CommandContext<CommandSource> context) {
    final CommandSource source = context.getSource();
    sendTotalProxyCount(source, this.server.getProxyId(), this.server.getPlayerCount());
    source.sendMessage(
        Component.translatable("velocity.command.plist-view-proxy", NamedTextColor.YELLOW)
            .arguments(Argument.string("alias", VelocityCommands.readAlias(context.getNodes()))));
    return Command.SINGLE_SUCCESS;
  }

  private Optional<String> validateProxy(final String proxyName, final CommandSource source) {
    return server.getRedis().getProxyService().getAllProxyIds().stream()
        .filter(proxyId -> proxyId.equalsIgnoreCase(proxyName))
        .findFirst()
        .or(() -> {
          source.sendMessage(Component.translatable("velocity.command.proxy-does-not-exist", NamedTextColor.RED)
              .arguments(Component.text(proxyName)));
          return Optional.empty();
        });
  }

  private int serverCount(final CommandContext<CommandSource> context) {
    final String proxyName = getString(context, PROXY_ARG);
    final String serverName = getString(context, SERVER_ARG);

    Optional<String> validatedProxy = validateProxy(proxyName, context.getSource());
    if (validatedProxy.isEmpty()) {
      return Command.SINGLE_SUCCESS;
    }

    if ("all".equalsIgnoreCase(serverName)) {
      List<PlayerEntry> allPlayers = new ArrayList<>();
      for (RegisteredServer registeredServer : server.getAllServers()) {
        List<PlayerEntry> serverPlayers =
            new ArrayList<>(this.server.getRedis().getPlayerService().getPlayerEntriesOnProxy(validatedProxy.get()));
        serverPlayers.removeIf(player -> !player.getServerName().equalsIgnoreCase(registeredServer.getServerInfo().getName()));
        allPlayers.addAll(serverPlayers);
        sendServerPlayers(context.getSource(), validatedProxy.get(), registeredServer, true);
      }

      sendTotalProxyCount(context.getSource(), validatedProxy.get(), allPlayers.size());
      return Command.SINGLE_SUCCESS;
    }

    Optional<RegisteredServer> validatedServer = validateServer(serverName, context.getSource());
    if (validatedServer.isEmpty()) {
      return Command.SINGLE_SUCCESS;
    }

    sendServerPlayers(context.getSource(), validatedProxy.get(), validatedServer.get(), false);
    return Command.SINGLE_SUCCESS;
  }

  private int proxyCount(final CommandContext<CommandSource> context) {
    final String proxyName = getString(context, PROXY_ARG);

    Optional<String> validatedProxy = validateProxy(proxyName, context.getSource());
    if (validatedProxy.isEmpty()) {
      return Command.SINGLE_SUCCESS;
    }

    final List<PlayerEntry> proxyPlayers = server.getRedis().getPlayerService().getPlayerEntriesOnProxy(validatedProxy.get());
    sendTotalProxyCount(context.getSource(), validatedProxy.get(), proxyPlayers.size());
    return Command.SINGLE_SUCCESS;
  }

  private Optional<RegisteredServer> validateServer(final String serverName, final CommandSource source) {
    return server.getAllServers().stream()
        .filter(registeredServer -> registeredServer.getServerInfo().getName().equalsIgnoreCase(serverName))
        .findFirst()
        .or(() -> {
          source.sendMessage(Component.translatable("velocity.command.server-does-not-exist", NamedTextColor.RED)
              .arguments(Component.text(serverName)));
          return Optional.empty();
        });
  }

  private void sendTotalProxyCount(final CommandSource target, final String proxyId, final int online) {
    final TranslatableComponent.Builder msg = Component.translatable()
        .key(online == 1
            ? "velocity.command.plist-player-singular"
            : "velocity.command.plist-player-plural"
        ).color(NamedTextColor.YELLOW)
        .arguments(
            Argument.numeric("count", online),
            Argument.string("proxy", proxyId));
    target.sendMessage(msg.build());
  }

  private void sendServerPlayers(final CommandSource target,
                                 final String proxyId,
                                 final RegisteredServer server,
                                 final boolean fromAll) {
    List<Component> players = new ArrayList<>();
    int totalPlayers = 0;

    for (PlayerEntry playerEntry : this.server.getRedis().getPlayerService().getPlayerEntriesOnProxy(proxyId)) {
      if (server.getServerInfo().getName().equalsIgnoreCase(playerEntry.getServerName())) {
        players.add(Component.text(playerEntry.getUsername()));
        totalPlayers++;
      }
    }

    if (fromAll && totalPlayers == 0) {
      return;
    }

    Component playerList = players.stream()
        .reduce((a, b) -> a.append(Component.text(", ")).append(b))
        .orElse(Component.text(""));
    target.sendMessage(Component.translatable("velocity.command.plist-server")
        .arguments(
            Argument.string("server", server.getServerInfo().getName()),
            Argument.numeric("count", totalPlayers),
            Argument.component("players", playerList)
        )
    );
  }
}
