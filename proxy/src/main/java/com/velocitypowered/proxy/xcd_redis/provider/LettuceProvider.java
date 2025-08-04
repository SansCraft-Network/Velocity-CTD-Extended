package com.velocitypowered.proxy.xcd_redis.provider;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.xcd_redis.depot.Depot;
import com.velocitypowered.proxy.xcd_redis.depot.DepotEntry;
import com.velocitypowered.proxy.xcd_redis.packet.RedisPacket;
import com.velocitypowered.proxy.xcd_redis.packet.serialization.PacketSerializer;
import com.velocitypowered.proxy.xcd_redis.registration.ConsumerRouteRegistration;
import com.velocitypowered.proxy.xcd_redis.registration.RouteRegistration;
import com.velocitypowered.proxy.xcd_redis.transaction.Transaction;
import com.velocitypowered.proxy.xcd_redis.transaction.TransactionHandler;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Elmar Blume - 08/05/2025
 */
public final class LettuceProvider extends AbstractRedisProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(LettuceProvider.class);

  private final RedisClient client;
  private RedisPubSubAsyncCommands<String, String> publisher;

  /**
   * Constructs a new {@link LettuceProvider}
   *
   * @param config the {@link VelocityConfiguration.Redis} instance to use for connection credentials
   */
  public LettuceProvider(VelocityConfiguration.Redis config) {
    super();

    // Initialize the redis client from the config
    this.client = RedisClient.create(RedisURI.Builder.redis(config.getHost(), config.getPort())
            .withAuthentication(Objects.requireNonNullElse(config.getUsername(), ""),
                    Objects.requireNonNullElse(config.getPassword(), ""))
            .withSsl(config.isUseSsl()) 
            .build());
  }

  @Override
  public void restart() {
    // Close previous publisher connections
    if (this.publisher != null) this.publisher.getStatefulConnection().close();

    // Reconnect and initialize the publisher
    final StatefulRedisPubSubConnection<String, String> connection = this.client.connectPubSub();

    // Register the sent-packet listener
    connection.addListener(new RedisPubSubAdapter<>() {
      @Override
      public void message(String channel, String message) {
        if (!channel.equals(CHANNEL)) return;

        // Read the incoming packet from the message
        final RedisPacket redisPacket = PacketSerializer.deserialize(message);
        if (redisPacket == null) {
          LOGGER.warn("Received a null packet from channel '{}', ignoring", channel);
          return;
        }

        // Handle the one way packet
        if (redisPacket.isOneWay()) {
          handleOneWay(redisPacket);
          return;
        }

        if (!redisPacket.isReply()) {
          // Check if the packet could be part of a transaction
          final TransactionHandler<?, ?> transactionHandler = transactionHandlers.get(Preconditions.checkNotNull(
                  redisPacket.getTransactionType(), "transactionType is null"));
          if (transactionHandler == null) return;

          // Handle the transaction
          final RedisPacket replyPacket = transactionHandler.getReplyPacket(redisPacket);
          if (replyPacket == null) return;

          // Publish the reply packet if required (non-null)
          LettuceProvider.this.publish(replyPacket);
        } else {
          final UUID transactionId = Preconditions.checkNotNull(redisPacket.getTransactionId());

          // Check if the packet is part of a transaction
          final Transaction<?, ?> transaction = PENDING_TRANSACTIONS.remove(transactionId);
          if (transaction == null) {
            LOGGER.warn("Received a packet with transactionId '{}' but no transaction was found, ignoring",
                    transactionId);
            return;
          }

          // Complete the transaction
          transaction.complete(redisPacket);
        }
      }
    });

    this.publisher = connection.async();
    this.publisher.subscribe(CHANNEL);

    LOGGER.info("Connected to Lettuce Redis Server on channel '{}'", CHANNEL);
  }

  @Override
  public <T extends RedisPacket> void publish(@NotNull T packet) {
    if (this.publisher == null) {
      LOGGER.warn("Attempted to publish a packet to channel '{}' but the publisher is not initialized", CHANNEL);
      return;
    }

    // Publish the packet to the channel
    this.publisher.publish(CHANNEL, PacketSerializer.serialize(packet)).whenComplete((received, throwable) -> {
      if (throwable != null) {
        LOGGER.warn("Failed to publish packet to '{}' channel", CHANNEL, throwable);
      } else {
        LOGGER.info("Successfully published packet to '{}' channel, received by {} clients", CHANNEL, received);
      }
    });
  }

  @Override
  @Contract(value = "_ -> new", pure = true)
  public <K, V extends DepotEntry<K, V>> @NotNull Depot<K, V> createDepot(Class<V> valueClass) {
    return new LettuceDepot<>(valueClass);
  }

  @Override
  public boolean isConnected() {
    return this.publisher != null && this.publisher.getStatefulConnection().isOpen();
  }

  @SuppressWarnings("unchecked")
  private <T extends RedisPacket> void handleOneWay(@NotNull T redisPacket) {
    final RouteRegistration<T> routeRegistration = (RouteRegistration<T>) routeRegistrations.get(redisPacket.getType());
    if (routeRegistration == null) {
      LOGGER.warn("Received a packet of type '{}' from channel '{}', but no route registration exists, ignoring",
              redisPacket.getType(), CHANNEL);
      return;
    }

    try {
      if (routeRegistration instanceof ConsumerRouteRegistration<T> cRegistration) {
        cRegistration.getConsumer().accept(redisPacket);
      }
    } catch (Throwable ignored) {
      LOGGER.warn("Failed to handle one way packet of type '{}', ignoring", redisPacket.getType());
    }
  }

  public final class LettuceDepot<K, V extends DepotEntry<K, V>> implements Depot<K, V> {
    private static final Gson GSON = PacketSerializer.GSON;

    private final String name;
    private final Class<V> valueClass;
    private final RedisPubSubCommands<String, String> connection;

    public LettuceDepot(@NotNull Class<V> valueClass) {
      this.name = valueClass.getSimpleName().toLowerCase();
      this.valueClass = valueClass;
      this.connection = client.connectPubSub().sync();
    }

    @Override
    public boolean contains(@NotNull K key) {
      return this.connection.hexists(this.name, parseKey(key));
    }

    @Override
    public @Nullable V get(@NotNull K key) {
      final String data = this.connection.hget(this.name, parseKey(key));
      return data == null ? null : deserialize(data);
    }

    @Override
    public void upsert(@NotNull V value) {
      this.connection.hset(this.name, value.getKey(), serialize(value));
      value.setDepot(this);
    }

    @Override
    public @Nullable V remove(@NotNull K key) {
      if (this.connection.hexists(this.name, parseKey(key))) {
        final String data = this.connection.hget(this.name, parseKey(key));
        this.connection.hdel(this.name, parseKey(key));

        return data == null ? null : deserialize(data);
      }

      return null;
    }

    @Override
    public Collection<V> values() {
      return this.connection.hvals(this.name).stream().map(this::deserialize).toList();
    }

    @Override
    public Collection<String> keys() {
      return this.connection.hkeys(this.name);
    }

    private @NotNull String serialize(@NotNull V entry) {
      return GSON.toJson(entry, this.valueClass);
    }

    private @NotNull V deserialize(@NotNull String data) {
      final V entry = GSON.fromJson(data, this.valueClass);
      entry.setDepot(this);

      return entry;
    }

    private @NotNull String parseKey(K key) {
      return String.valueOf(key);
    }

  }
}
