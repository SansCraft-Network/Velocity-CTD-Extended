package com.velocityctd.proxy.connection.profile;

import com.velocitypowered.api.util.GameProfile;
import java.util.NoSuchElementException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class GameProfileResponse {

  static GameProfileResponse success(GameProfile gameProfile) {
    return new GameProfileResponse(gameProfile, Status.SUCCESS);
  }

  static GameProfileResponse error(Status errorStatus) {
    if (errorStatus.success()) {
      throw new IllegalArgumentException("Expected a non-successful status.");
    }

    return new GameProfileResponse(null, errorStatus);
  }

  @Nullable
  private final GameProfile gameProfile;

  @NonNull
  private final Status status;

  private GameProfileResponse(@Nullable GameProfile gameProfile, @NonNull Status status) {
    if (status.success() && gameProfile == null) {
      throw new IllegalArgumentException("Expected a non-null GameProfile for a successful status.");
    }
    if (!status.success() && gameProfile != null) {
      throw new IllegalArgumentException("Expected a null GameProfile for a non-successful status.");
    }

    this.gameProfile = gameProfile;
    this.status = status;
  }

  @NonNull
  public GameProfile gameProfile() {
    if (gameProfile == null) {
      throw new NoSuchElementException("No game profile fetched.");
    }

    return gameProfile;
  }

  @NonNull
  public Status status() {
    return status;
  }

  public enum Status {

    SUCCESS,
    ERROR_OFFLINE_USER,
    ERROR_AUTH_DOWN;

    public boolean success() {
      return this == SUCCESS;
    }
  }
}
