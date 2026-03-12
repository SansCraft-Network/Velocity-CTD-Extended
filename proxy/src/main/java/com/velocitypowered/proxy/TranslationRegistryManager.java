package com.velocitypowered.proxy;

import com.velocitypowered.proxy.util.ClosestLocaleMatcher;
import com.velocitypowered.proxy.util.ResourceUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TranslationRegistryManager {

  private static final Logger LOGGER = LogManager.getLogger(TranslationRegistryManager.class);

  /**
   * The {@link Key} used to register Velocity's translation source in the Adventure global translator.
   */
  private final Key translationRegistryKey;

  /**
   * Whether to log when registering translations.
   */
  private boolean log = true;

  TranslationRegistryManager(Key translationRegistryKey) {
    this.translationRegistryKey = translationRegistryKey;
  }

  TranslationRegistryManager() {
    this(Key.key("velocity", "translations"));
  }

  void unregisterTranslations() {
    for (final Translator source : GlobalTranslator.translator().sources()) {
      if (source.name().equals(this.translationRegistryKey)) {
        GlobalTranslator.translator().removeSource(source);
      }
    }
  }

  void registerTranslations() {
    final MiniMessageTranslationStore translationRegistry =
        MiniMessageTranslationStore.create(this.translationRegistryKey);
    translationRegistry.defaultLocale(Locale.US);

    try {
      ResourceUtils.visitResources(VelocityServer.class, path -> {
        if (log) {
          LOGGER.info("Loading localizations...");
        }

        final Path langPath = Path.of("lang");

        try {
          if (!Files.exists(langPath)) {
            Files.createDirectories(langPath);
          }

          try (Stream<Path> files = Files.walk(path)) {
            files.filter(Files::isRegularFile).forEach(src -> {
              final Path target = langPath.resolve(src.getFileName().toString());
              if (Files.notExists(target)) {
                try (InputStream is = Files.newInputStream(src)) {
                  Files.copy(is, target);
                  if (log) {
                    LOGGER.info("Restored missing translation file {}", target.getFileName());
                  }
                } catch (IOException e) {
                  LOGGER.error("Failed copying translation file {}", target.getFileName(), e);
                }
              }
            });
          }

          try (Stream<Path> langFiles = Files.walk(langPath)) {
            langFiles.filter(Files::isRegularFile).forEach(file -> {
              try {
                String localePart = com.google.common.io.Files
                    .getNameWithoutExtension(file.getFileName().toString());
                if (localePart.startsWith("messages")) {
                  localePart = localePart.substring("messages".length());
                }

                if (localePart.startsWith("_")) {
                  localePart = localePart.substring(1);
                }

                final Locale locale = localePart.isBlank()
                    ? Locale.US
                    : Locale.forLanguageTag(localePart.replace('_', '-'));

                translationRegistry.registerAll(locale, file, false);
                ClosestLocaleMatcher.INSTANCE.registerKnown(locale);
              } catch (Exception e) {
                LOGGER.error("Failed registering translations from {}", file, e);
              }
            });
          }
        } catch (Exception e) {
          LOGGER.error("Encountered an error whilst loading translations", e);
        }
      }, "com", "velocitypowered", "proxy", "l10n");
    } catch (IOException e) {
      LOGGER.error("Encountered an I/O error whilst loading translations", e);
      return;
    }

    GlobalTranslator.translator().addSource(translationRegistry);
  }

  /**
   * Disables logging (logging can't be turned on after this).
   */
  void noLogging() {
    log = false;
  }
}
