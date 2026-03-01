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

package com.velocitypowered.proxy.permission;

import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.advanced.AdvancedPermissionResolver;
import com.velocitypowered.api.permission.advanced.AdvancedPermissionResolverProvider;
import com.velocitypowered.api.permission.advanced.SimplePermissionResolverAdapter;
import com.velocitypowered.proxy.plugin.PluginClassLoader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.ServiceLoader;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Factory for producing an {@link AdvancedPermissionResolver} for a given {@link PermissionSubject}.
 *
 * <p>This factory supports an optional, runtime-discovered integration that is embedded inside the
 * proxy jar (as a nested jar resource). When present, the embedded integration jar is extracted to
 * a temporary file and loaded via a dedicated {@link PluginClassLoader}. The implementation is then
 * discovered using {@link ServiceLoader} by locating an {@link AdvancedPermissionResolverProvider}.
 *
 * <p>If no provider can be loaded (e.g., the embedded jar is missing, cannot be extracted, or the
 * provider reports it is unavailable), this factory falls back to {@link SimplePermissionResolverAdapter}.
 *
 * <p>The provider lookup and jar extraction are performed at most once per JVM. The result is cached
 * (including the "no provider available" outcome) to avoid repeated I/O and class loading overhead.
 */
public final class AdvancedPermissionResolverAdapterFactory {

  private static final String INTEGRATION_JAR_RESOURCE =
      "META-INF/velocityctd/integrations/velocity-luckperms-integration.jar";

  private static volatile boolean hasLoadedProvider = false;
  private static volatile @Nullable AdvancedPermissionResolverProvider cachedProvider = null;

  private AdvancedPermissionResolverAdapterFactory() {
  }

  /**
   * Creates an {@link AdvancedPermissionResolver} for the supplied {@link PermissionSubject}.
   *
   * <p>If an {@link AdvancedPermissionResolverProvider} is available via the embedded integration,
   * this method delegates to {@link AdvancedPermissionResolverProvider#createResolver(PermissionSubject, PermissionFunction)}.
   * Otherwise, it returns a {@link SimplePermissionResolverAdapter} wrapping {@code delegate}.
   *
   * @param permissionSubject the subject the resolver will evaluate permissions for
   * @param delegate the base permission function to delegate to when the advanced resolver is not available
   * @return an advanced resolver when an integration is available; otherwise a simple adapter
   */
  public static AdvancedPermissionResolver createPermissionResolverAdapter(
      PermissionSubject permissionSubject,
      PermissionFunction delegate
  ) {
    return getCachedProvider()
        .map(provider -> provider.createResolver(permissionSubject, delegate))
        .orElseGet(() -> new SimplePermissionResolverAdapter(delegate));
  }

  /**
   * Returns the cached {@link AdvancedPermissionResolverProvider}, loading it once if necessary.
   *
   * <p>This method is thread-safe and caches both success and failure:
   * once the initial lookup completes, subsequent calls will not repeat extraction, class loading,
   * or {@link ServiceLoader} scanning. If no provider is available, {@link Optional#empty()} is returned.
   *
   * @return an {@link Optional} containing the cached provider if available; otherwise empty
   */
  private static Optional<AdvancedPermissionResolverProvider> getCachedProvider() {
    if (hasLoadedProvider) {
      return Optional.ofNullable(cachedProvider);
    }

    synchronized (AdvancedPermissionResolverAdapterFactory.class) {
      // Check again in lock
      if (hasLoadedProvider) {
        return Optional.ofNullable(cachedProvider);
      }

      cachedProvider = loadProviderOnce().orElse(null);
      hasLoadedProvider = true;

      return Optional.ofNullable(cachedProvider);
    }
  }

  /**
   * Attempts to load an {@link AdvancedPermissionResolverProvider} from the embedded integration jar.
   *
   * <p>This method:
   * <ol>
   *   <li>Extracts the embedded jar resource to a temporary file,</li>
   *   <li>Creates a {@link PluginClassLoader} for that jar,</li>
   *   <li>Uses {@link ServiceLoader} to locate provider implementations,</li>
   *   <li>Returns the first provider that reports {@link AdvancedPermissionResolverProvider#isAvailable()}.</li>
   * </ol>
   *
   * <p>The provider implementation is expected to be registered using Java's SPI mechanism
   * (i.e., {@code META-INF/services/...}) inside the embedded jar.
   *
   * @return an {@link Optional} containing a usable provider, or empty if none can be loaded
   */
  private static Optional<AdvancedPermissionResolverProvider> loadProviderOnce() {
    URL jarUrl = extractEmbeddedJarToTempUrl(INTEGRATION_JAR_RESOURCE);
    if (jarUrl == null) {
      return Optional.empty();
    }

    ClassLoader integrationLoader = new PluginClassLoader(new URL[] {jarUrl});

    return ServiceLoader.load(AdvancedPermissionResolverProvider.class, integrationLoader)
        .stream()
        .map(ServiceLoader.Provider::get)
        .filter(AdvancedPermissionResolverProvider::isAvailable)
        .findFirst();
  }

  /**
   * Extracts an embedded jar resource from the proxy jar to a temporary file and returns its {@link URL}.
   *
   * <p>The extracted file is marked for deletion on JVM exit via {@link java.io.File#deleteOnExit()}.
   * If the resource cannot be found or cannot be written to the temp directory, this method returns {@code null}.
   *
   * <p>The returned URL is suitable for constructing a class loader (e.g., {@link PluginClassLoader})
   * that can load classes and resources from the extracted jar.
   *
   * @param resourcePath the classpath resource path to the embedded jar within the proxy jar
   * @return a {@link URL} to the extracted temporary jar file, or {@code null} if extraction fails
   */
  private static URL extractEmbeddedJarToTempUrl(String resourcePath) {
    ClassLoader cl = AdvancedPermissionResolverAdapterFactory.class.getClassLoader();
    if (cl == null) {
      cl = ClassLoader.getSystemClassLoader();
    }

    try (InputStream in = cl.getResourceAsStream(resourcePath)) {
      if (in == null) {
        return null;
      }

      Path tmp = Files.createTempFile("velocity-integration-", ".jar");
      tmp.toFile().deleteOnExit();

      Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
      return tmp.toUri().toURL();
    } catch (IOException e) {
      return null;
    }
  }
}
