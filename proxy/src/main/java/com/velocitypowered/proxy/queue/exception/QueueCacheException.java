package com.velocitypowered.proxy.queue.exception;

/**
 * @author Elmar Blume - 03/04/2025
 */
public final class QueueCacheException extends RuntimeException {

  /**
   * Constructs a new {@link QueueCacheException}
   *
   * @param serverName the server name
   */
  public QueueCacheException(String serverName) {
    super("Attempted to fetch queue for invalid server: '%s'".formatted(serverName));
  }

}
