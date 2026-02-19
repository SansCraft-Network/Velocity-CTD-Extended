package com.velocitypowered.proxy.config.migration;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import org.apache.logging.log4j.Logger;

public final class ForcedHostAsFallbackMigration implements ConfigurationMigration {

  private static final String OPTION_KEY = "forced-hosts.forced-host-as-fallback";

  private static final boolean OPTION_DEFAULT_VALUE = true;

  private static final String OPTION_COMMENT = """
      # Whether to use the configured forced hosts as fallback (try) servers
      # if a player joins through a forced host that's configured.""";

  @Override
  public boolean shouldMigrate(CommentedFileConfig config) {
    return !config.contains(OPTION_KEY);
  }

  @Override
  public void migrate(CommentedFileConfig config, Logger logger) {
    config.set(OPTION_KEY, OPTION_DEFAULT_VALUE);
    config.setComment(OPTION_KEY, OPTION_COMMENT);
  }
}
