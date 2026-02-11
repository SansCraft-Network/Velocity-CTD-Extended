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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.command.VelocityCommands;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import java.util.function.Function;
import net.kyori.adventure.text.Component;

/**
 * Implements Velocity-CTD's {@code /<server_name>} commands.
 */
public class SlashServerCommand implements BuiltinCommand {

  public static Function<VelocityServer, SlashServerCommand> factory(String serverName, String commandLabel) {
    return proxyServer -> new SlashServerCommand(proxyServer, serverName, commandLabel);
  }

  private final VelocityServer server;
  private final VelocityRegisteredServer registeredServer;
  private final String commandLabel;

  public SlashServerCommand(VelocityServer server, String targetServerName, String commandLabel) {
    this.server = server;
    this.registeredServer = (VelocityRegisteredServer) this.server.getServer(targetServerName)
            .orElseThrow(() -> new IllegalArgumentException("Target server '" + targetServerName + "' does not exist."));
    this.commandLabel = commandLabel;
  }

  @Override
  public String label() {
    return commandLabel;
  }

  @Override
  public BrigadierCommand build() {
    LiteralArgumentBuilder<CommandSource> rootNode = BrigadierCommand
            .literalArgumentBuilder(label())
            .requires(src -> VelocityCommands.checkServerPermissions(registeredServer, src))
            .executes(this::send);

    return new BrigadierCommand(rootNode);
  }

  private int send(CommandContext<CommandSource> ctx) {
    Player player = (Player) ctx.getSource();

    ServerConnection connection = player.getCurrentServer().orElse(null);
    if (connection != null && connection.getServerInfo().getName()
            .equalsIgnoreCase(registeredServer.getServerInfo().getName())) {
      player.sendMessage(Component.translatable("velocity.command.slashserver.already-connected"));
      return -1;
    }

    if (server.getConfiguration().getQueue().getNoQueueServers() == null
            || server.getConfiguration().getQueue().getNoQueueServers().contains(registeredServer.getServerInfo().getName())
            || !server.isQueueEnabled()
            || player.hasPermission("velocity.queue.bypass")) {
      player.createConnectionRequest(registeredServer).connectWithIndication();
      return Command.SINGLE_SUCCESS;
    }

    server.getQueueManager().queue(player, registeredServer);
    return Command.SINGLE_SUCCESS;
  }
}
