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
