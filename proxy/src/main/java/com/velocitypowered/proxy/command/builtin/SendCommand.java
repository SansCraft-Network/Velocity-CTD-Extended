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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.command.VelocityCommands;
import com.velocitypowered.proxy.redis.VelocityRedis;
import com.velocitypowered.proxy.redis.impl.depot.PlayerEntry;
import com.velocitypowered.proxy.redis.impl.packet.VelocitySwitchServer;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;

/**
 * Implements Velocity's {@code /send} command.
 */
public class SendCommand implements BuiltinCommand {

  private static final String SERVER_ARG = "server";

  private static final String PLAYER_ARG = "player";

  private final VelocityServer server;

  private final VelocityRedis redis;

  public SendCommand(VelocityServer server) {
    this.server = server;
    this.redis = server.getRedis();
  }

  @Override
  public String label() {
    return "send";
  }

  @Override
  public BrigadierCommand build() {
    if (server.isRedisEnabled()) {
      return registerMultiProxy();
    }

    LiteralArgumentBuilder<CommandSource> rootNode = BrigadierCommand
            .literalArgumentBuilder(label())
            .requires(source ->
                    source.getPermissionValue("velocity.command.send") == Tristate.TRUE)
            .executes(ctx -> VelocityCommands.emitUsage(ctx, label()));
    RequiredArgumentBuilder<CommandSource, String> playerNode = BrigadierCommand
            .requiredArgumentBuilder(PLAYER_ARG, StringArgumentType.word())
            .suggests((context, builder) -> {
              String argument = context.getArguments().containsKey(PLAYER_ARG)
                      ? context.getArgument(PLAYER_ARG, String.class)
                      : "";

              for (Player player : server.getAllPlayers()) {
                String playerName = player.getUsername();
                if (playerName.regionMatches(true, 0, argument, 0, argument.length())) {
                  builder.suggest(playerName);
                }
              }

              if ("all".regionMatches(true, 0, argument, 0, argument.length())) {
                builder.suggest("all");
              }

              if ("current".regionMatches(true, 0, argument, 0, argument.length())
                      && context.getSource() instanceof Player) {
                builder.suggest("current");
              }

              if (argument.isEmpty() || argument.startsWith("+")) {
                for (RegisteredServer server : server.getAllServers()) {
                  String serverName = server.getServerInfo().getName();

                  if (serverName.regionMatches(true, 0, argument, 1, argument.length() - 1)) {
                    builder.suggest("+" + serverName);
                  }
                }
              }

              return builder.buildFuture();
            })
            .executes(ctx -> VelocityCommands.emitUsage(ctx, label()));
    ArgumentCommandNode<CommandSource, String> serverNode = BrigadierCommand
            .requiredArgumentBuilder(SERVER_ARG, StringArgumentType.word())
            .suggests((context, builder) -> {
              String argument = context.getArguments().containsKey(SERVER_ARG)
                      ? context.getArgument(SERVER_ARG, String.class)
                      : "";

              for (RegisteredServer server : server.getAllServers()) {
                String serverName = server.getServerInfo().getName();
                if (serverName.regionMatches(true, 0, argument, 0, argument.length())) {
                  builder.suggest(server.getServerInfo().getName());
                }
              }

              return builder.buildFuture();
            })
            .executes(this::send)
            .build();

    playerNode.then(serverNode);
    rootNode.then(playerNode.build());
    return new BrigadierCommand(rootNode);
  }

  /**
   * Handles registering the command when Redis is enabled.
   */
  public BrigadierCommand registerMultiProxy() {
    LiteralArgumentBuilder<CommandSource> rootNode = BrigadierCommand
            .literalArgumentBuilder(label())
            .requires(source ->
                    source.getPermissionValue("velocity.command.send") == Tristate.TRUE)
            .executes(ctx -> VelocityCommands.emitUsage(ctx, label()));
    RequiredArgumentBuilder<CommandSource, String> playerNode = BrigadierCommand
            .requiredArgumentBuilder(PLAYER_ARG, StringArgumentType.word())
            .suggests((context, builder) -> {
              String argument = context.getArguments().containsKey(PLAYER_ARG)
                      ? context.getArgument(PLAYER_ARG, String.class)
                      : "";

              for (PlayerEntry playerEntry : redis.getPlayerService().getAll()) {
                String playerName = playerEntry.getUsername();
                if (playerName.regionMatches(true, 0, argument, 0, argument.length())) {
                  builder.suggest(playerName);
                }
              }

              if ("all".regionMatches(true, 0, argument, 0, argument.length())) {
                builder.suggest("all");
              }

              if ("current".regionMatches(true, 0, argument, 0, argument.length())
                      && context.getSource() instanceof Player) {
                builder.suggest("current");
              }

              if (argument.isEmpty() || argument.startsWith("+")) {
                for (RegisteredServer server : server.getAllServers()) {
                  String serverName = server.getServerInfo().getName();
                  if (serverName.regionMatches(true, 0, argument, 1, argument.length() - 1)) {
                    builder.suggest("+" + serverName);
                  }
                }
              }

              return builder.buildFuture();
            })
            .executes(ctx -> VelocityCommands.emitUsage(ctx, label()));
    ArgumentCommandNode<CommandSource, String> serverNode = BrigadierCommand
            .requiredArgumentBuilder(SERVER_ARG, StringArgumentType.word())
            .suggests((context, builder) -> {
              String argument = context.getArguments().containsKey(SERVER_ARG)
                      ? context.getArgument(SERVER_ARG, String.class)
                      : "";

              for (RegisteredServer server : server.getAllServers()) {
                String serverName = server.getServerInfo().getName();
                if (serverName.regionMatches(true, 0, argument, 0, argument.length())) {
                  builder.suggest(server.getServerInfo().getName());
                }
              }

              return builder.buildFuture();
            })
            .executes(this::send)
            .build();

    playerNode.then(serverNode);
    rootNode.then(playerNode.build());

    return new BrigadierCommand(rootNode);
  }

  private int send(CommandContext<CommandSource> context) {
    if (server.isRedisEnabled()) {
      return sendMultiProxy(context);
    }

    String serverName = context.getArgument(SERVER_ARG, String.class);
    String player = context.getArgument(PLAYER_ARG, String.class);

    Optional<RegisteredServer> maybeServer = server.getServer(serverName);

    if (maybeServer.isEmpty()) {
      context.getSource().sendMessage(
              CommandMessages.SERVER_DOES_NOT_EXIST.arguments(Component.text(serverName))
      );

      return 0;
    }

    RegisteredServer targetServer = maybeServer.get();

    Optional<Player> maybePlayer = server.getPlayer(player);
    if (maybePlayer.isEmpty()
            && !Objects.equals(player, "all")
            && !Objects.equals(player, "current")
            && !player.startsWith("+")) {
      context.getSource().sendMessage(
              CommandMessages.PLAYER_NOT_FOUND.arguments(Argument.string("player", player))
      );

      return 0;
    }

    if (Objects.equals(player, "all")) {
      for (Player p : server.getAllPlayers()) {
        p.createConnectionRequest(targetServer).fireAndForget();
      }
      int globalCount = server.getAllPlayers().size();
      context.getSource().sendMessage(Component.translatable(globalCount == 1
                      ? "velocity.command.send-all-singular" : "velocity.command.send-all-plural")
              .arguments(
                      Argument.numeric("count", globalCount),
                      Argument.string("server", targetServer.getServerInfo().getName())));
      return Command.SINGLE_SUCCESS;
    }

    if (Objects.equals(player, "current")) {
      if (!(context.getSource() instanceof Player source)) {
        context.getSource().sendMessage(CommandMessages.PLAYERS_ONLY);
        return 0;
      }

      Optional<ServerConnection> connectedServer = source.getCurrentServer();
      if (connectedServer.isPresent()) {
        if (maybeServer.get().getServerInfo().getName().equalsIgnoreCase(connectedServer.get().getServerInfo().getName())) {
          context.getSource().sendMessage(Component.translatable("velocity.command.send-same-server"));
          return -1;
        }

        Collection<Player> players = connectedServer.get().getServer().getPlayersConnected();
        for (Player p : players) {
          p.createConnectionRequest(maybeServer.get()).fireAndForget();
        }
        context.getSource().sendMessage(Component.translatable(players.size() == 1
                        ? "velocity.command.send-server-singular" : "velocity.command.send-server-plural")
                .arguments(
                        Argument.numeric("count", players.size()),
                        Argument.string("from", connectedServer.get().getServerInfo().getName()),
                        Argument.string("to", targetServer.getServerInfo().getName())));
        return Command.SINGLE_SUCCESS;
      }

      return 0;
    }

    if (player.startsWith("+")) {
      ServerResult result = findServer(player.substring(1));

      if (result.bestMatch().isEmpty()) {
        context.getSource().sendMessage(CommandMessages.SERVER_DOES_NOT_EXIST.arguments(Component.text(player.substring(1))));
        return 0;
      }

      if (result.hasMultipleMatches()) {
        context.getSource().sendMessage(CommandMessages.SERVER_MULTIPLE_MATCH);
        return 0;
      }

      RegisteredServer sourceServer = result.bestMatch().get();
      sendPlayersFromServer(context, sourceServer, targetServer);
      return Command.SINGLE_SUCCESS;
    }

    // The player at this point must be present
    Player player0 = maybePlayer.orElseThrow();
    sendPlayer(context, player0, targetServer);
    return Command.SINGLE_SUCCESS;
  }

  private void sendPlayer(CommandContext<CommandSource> context, Player player0,
                          RegisteredServer targetServer) {
    ServerConnection current = player0.getCurrentServer().orElse(null);
    if (current != null && current.getServerInfo().getName().equalsIgnoreCase(targetServer.getServerInfo().getName())) {
      context.getSource().sendMessage(Component.translatable("velocity.command.send-same-server"));
      return;
    }

    if (player0.getCurrentServer().isPresent() && player0.getCurrentServer().get().getServer().equals(targetServer)) {
      context.getSource().sendMessage(Component.translatable("velocity.command.send-player-none")
              .arguments(
                      Argument.string("player", player0.getUsername()),
                      Argument.string("server", targetServer.getServerInfo().getName())));
    } else {
      player0.createConnectionRequest(targetServer).fireAndForget();
      context.getSource().sendMessage(Component.translatable("velocity.command.send-player")
              .arguments(
                      Argument.string("player", player0.getUsername()),
                      Argument.string("server", targetServer.getServerInfo().getName())));
    }
  }

  private void sendPlayersFromServer(CommandContext<CommandSource> context, RegisteredServer server,
                                     RegisteredServer targetServer) {

    if (server.getServerInfo().getName().equalsIgnoreCase(targetServer.getServerInfo().getName())) {
      context.getSource().sendMessage(Component.translatable("velocity.command.send-same-server"));
      return;
    }

    int playerSize = server.getPlayersConnected().size();
    String name = server.getServerInfo().getName();

    if (playerSize == 0) {
      context.getSource().sendMessage(Component.translatable("velocity.command.send-server-none")
              .arguments(
                      Argument.string("server", name),
                      Argument.string("to", targetServer.getServerInfo().getName())));
      return;
    }

    for (Player targetPlayer : server.getPlayersConnected()) {
      targetPlayer.createConnectionRequest(targetServer).fireAndForget();
    }

    context.getSource().sendMessage(Component.translatable(playerSize == 1
                    ? "velocity.command.send-server-singular" : "velocity.command.send-server-plural")
            .arguments(
                    Argument.numeric("count", playerSize),
                    Argument.string("from", name),
                    Argument.string("to", targetServer.getServerInfo().getName())));
  }

  private int sendMultiProxy(CommandContext<CommandSource> context) {
    String serverName = context.getArgument(SERVER_ARG, String.class);
    String player = context.getArgument(PLAYER_ARG, String.class);

    Optional<RegisteredServer> maybeServer = server.getServer(serverName);

    if (maybeServer.isEmpty()) {
      context.getSource().sendMessage(
              CommandMessages.SERVER_DOES_NOT_EXIST.arguments(Component.text(serverName))
      );

      return 0;
    }

    RegisteredServer targetServer = maybeServer.get();

    if (this.redis.getPlayerService().isPlayerOnline(player)
            && !Objects.equals(player, "all")
            && !Objects.equals(player, "current")
            && !player.startsWith("+")) {
      context.getSource().sendMessage(
              CommandMessages.PLAYER_NOT_FOUND.arguments(Argument.string("player", player))
      );

      return 0;
    }

    if (Objects.equals(player, "all")) {
      List<PlayerEntry> list = this.redis.getPlayerService().getAll();
      for (PlayerEntry playerEntry : list) {
        new VelocitySwitchServer(playerEntry.getUsername(), targetServer.getServerInfo().getName())
                .publish();
      }

      int globalCount = list.size();
      context.getSource().sendMessage(Component.translatable(globalCount == 1
                      ? "velocity.command.send-all-singular" : "velocity.command.send-all-plural")
              .arguments(
                      Argument.numeric("count", globalCount),
                      Argument.string("server", targetServer.getServerInfo().getName())));
      return Command.SINGLE_SUCCESS;
    }

    if (Objects.equals(player, "current")) {
      if (!(context.getSource() instanceof Player source)) {
        context.getSource().sendMessage(CommandMessages.PLAYERS_ONLY);
        return 0;
      }

      Optional<ServerConnection> connectedServer = source.getCurrentServer();
      if (connectedServer.isPresent()) {
        if (maybeServer.get().getServerInfo().getName().equalsIgnoreCase(connectedServer.get().getServerInfo().getName())) {
          context.getSource().sendMessage(Component.translatable("velocity.command.send-same-server"));
          return -1;
        }
        int amountDone = 0;
        List<PlayerEntry> list = this.redis.getPlayerService().getAll();
        for (PlayerEntry playerEntry : list) {
          if (playerEntry.getServerName().equalsIgnoreCase(connectedServer.get().getServerInfo().getName())) {
            new VelocitySwitchServer(playerEntry.getUsername(),
                    connectedServer.get().getServerInfo().getName())
                    .publish();
            amountDone++;
          }
        }

        context.getSource().sendMessage(Component.translatable(amountDone == 1
                        ? "velocity.command.send-server-singular" : "velocity.command.send-server-plural")
                .arguments(
                        Argument.numeric("count", amountDone),
                        Argument.string("from", connectedServer.get().getServerInfo().getName()),
                        Argument.string("to", targetServer.getServerInfo().getName())));
        return Command.SINGLE_SUCCESS;
      }

      return 0;
    }

    if (player.startsWith("+")) {

      ServerResult result = findServer(player.substring(1));

      if (result.bestMatch().isEmpty()) {
        context.getSource().sendMessage(CommandMessages.SERVER_DOES_NOT_EXIST.arguments(Component.text(player.substring(1))));
        return 0;
      }

      if (result.hasMultipleMatches()) {
        context.getSource().sendMessage(CommandMessages.SERVER_MULTIPLE_MATCH);
        return 0;
      }

      RegisteredServer sourceServer = result.bestMatch().get();
      sendPlayersFromServerMultiProxy(context, sourceServer, targetServer);
      return Command.SINGLE_SUCCESS;
    }

    // The player at this point must be present
    sendPlayerMultiProxy(context, player, targetServer);
    return Command.SINGLE_SUCCESS;
  }

  private void sendPlayerMultiProxy(CommandContext<CommandSource> context, String playerInput,
                                    RegisteredServer targetServer) {

    PlayerEntry playerEntry = redis.getPlayerService().getPlayerEntry(playerInput);

    String correctName = Objects.requireNonNull(playerEntry).getUsername();
    boolean alreadyConnected = playerEntry.getServerName().equalsIgnoreCase(targetServer.getServerInfo().getName());

    if (alreadyConnected) {
      context.getSource().sendMessage(Component.translatable("velocity.command.send-player-none")
              .arguments(
                      Argument.string("player", correctName),
                      Argument.string("server", targetServer.getServerInfo().getName())));
    } else {
      new VelocitySwitchServer(correctName, targetServer.getServerInfo().getName())
              .publish();
      context.getSource().sendMessage(Component.translatable("velocity.command.send-player")
              .arguments(
                      Argument.string("player", correctName),
                      Argument.string("server", targetServer.getServerInfo().getName())));
    }
  }

  private void sendPlayersFromServerMultiProxy(CommandContext<CommandSource> context, RegisteredServer server,
                                               RegisteredServer targetServer) {
    String name = server.getServerInfo().getName();

    if (name.equalsIgnoreCase(targetServer.getServerInfo().getName())) {
      context.getSource().sendMessage(Component.translatable("velocity.command.send-same-server"));
      return;
    }

    int amountDone = 0;
    List<PlayerEntry> list = this.redis.getPlayerService().getAll();
    for (PlayerEntry playerEntry : list) {
      if (playerEntry.getServerName().equalsIgnoreCase(name)) {
        new VelocitySwitchServer(playerEntry.getUsername(), targetServer.getServerInfo().getName())
                .publish();
        amountDone++;
      }
    }

    if (amountDone == 0) {
      context.getSource().sendMessage(Component.translatable("velocity.command.send-server-none")
              .arguments(
                      Argument.string("server", name),
                      Argument.string("to", targetServer.getServerInfo().getName())));
      return;
    }
    for (Player targetPlayer : server.getPlayersConnected()) {
      targetPlayer.createConnectionRequest(targetServer).fireAndForget();
    }
    context.getSource().sendMessage(Component.translatable(amountDone == 1
                    ? "velocity.command.send-server-singular" : "velocity.command.send-server-plural")
            .arguments(
                    Argument.numeric("count", amountDone),
                    Argument.string("from", name),
                    Argument.string("to", targetServer.getServerInfo().getName())));
  }

  private ServerResult findServer(String serverName) {
    Collection<RegisteredServer> servers = server.getAllServers();
    String lowerServerName = serverName.toLowerCase();

    Optional<RegisteredServer> bestMatch = Optional.empty();
    boolean multipleMatches = false;

    for (RegisteredServer server : servers) {

      String lowerName = server.getServerInfo().getName().toLowerCase();

      if (lowerName.equals(lowerServerName)) {
        bestMatch = Optional.of(server);
        break;
      }

      if (lowerName.contains(lowerServerName)) {
        if (bestMatch.isPresent()) {
          multipleMatches = true;
          break;
        }

        bestMatch = Optional.of(server);
      }
    }

    return new ServerResult(bestMatch, multipleMatches);
  }

  private record ServerResult(Optional<RegisteredServer> bestMatch, boolean multipleMatches) {

    public boolean hasMultipleMatches() {
      return multipleMatches;
    }
  }
}
