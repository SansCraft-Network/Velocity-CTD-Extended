package com.velocitypowered.proxy.xcd_queue.model;

import java.util.UUID;

/**
 * Represents an entry in a {@link Queue} for a specific player
 *
 * @author Elmar Blume - 03/04/2025
 */
public final class QueueEntry {

  private final UUID playerUniqueId;
  private int priority;

  public QueueEntry(UUID playerUniqueId) {
    this.playerUniqueId = playerUniqueId;
  }
}
