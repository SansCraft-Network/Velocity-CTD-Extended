package com.velocitypowered.proxy.xcd_queue;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.xcd_queue.model.QueuePlayer;
import com.velocitypowered.proxy.xcd_queue.model.ServerStatus;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;

/**
 * @author Elmar Blume - 10/10/2025
 */
public sealed interface Queue permits AbstractQueue {

  void enqueue(final Player player);

  void dequeue(final Player player, boolean maxRetriesReached);

  boolean contains(final Player player);

  void transferFirst(final QueuePlayer queuePlayer);

  QueuePlayer pollFirst();

  @NotNull
  @Unmodifiable
  Collection<QueuePlayer> getQueuePlayers();

  int getPosition(final Player player);

  VelocityRegisteredServer getBackendInstance();

  String getName();

  int size();

  boolean isOnline();

  boolean isPaused();

  boolean isFull();

  ServerStatus getStatus();

  void setStatus(final ServerStatus status);

  void stop();

  Component calculateEta(final long position);

  Component getActionBarComponent(final QueuePlayer queuePlayer);
}
