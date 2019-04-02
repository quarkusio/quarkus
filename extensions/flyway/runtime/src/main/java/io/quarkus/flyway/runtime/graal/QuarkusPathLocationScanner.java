/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.flyway.runtime.graal;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.internal.resource.LoadableResource;
import org.flywaydb.core.internal.resource.classpath.ClassPathResource;
import org.flywaydb.core.internal.scanner.classpath.ResourceAndClassScanner;

public final class QuarkusPathLocationScanner implements ResourceAndClassScanner {
    private static final Log LOG = LogFactory.getLog(QuarkusPathLocationScanner.class);
    // TODO: this should be configurable, using flyway default
    private final static String DEFAULT_NATIVE_LOCATION = "db/migration";
    private static Set<String> ALL_SQL_RESOURCES;

    static {
        try {
            ALL_SQL_RESOURCES = discoverApplicationMigrations();
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Could not discover Flyway migrations", e);
        }
    }

    public QuarkusPathLocationScanner(Location location) {
        if (!location.getPath().equals(DEFAULT_NATIVE_LOCATION)) {
            LOG.error("Invalid migrations location: " + location.getPath() + ". Flyway migrations must be located in  "
                    + DEFAULT_NATIVE_LOCATION);
        }
    }

    private static Set<String> discoverApplicationMigrations() throws IOException, URISyntaxException {
        Set<String> resources = new HashSet<>();
        try {
            Enumeration<URL> migrations = Thread.currentThread().getContextClassLoader().getResources(
                    DEFAULT_NATIVE_LOCATION);
            while (migrations.hasMoreElements()) {
                URL path = migrations.nextElement();
                LOG.debug("Adding application migrations in path: " + path);
                if ("jar".equals(path.getProtocol())) {
                    resources = scanInJar(path);
                } else {
                    throw new IllegalStateException("Expecting jar file. Protocol not supported:" + path.getProtocol());
                }
            }
            return resources;
        } catch (IOException e) {
            throw e;
        }
    }

    public static Set<String> scanInJar(URL locationUrl) {
        JarFile jarFile;
        try {
            jarFile = getJarFromUrl(locationUrl);
        } catch (IOException e) {
            LOG.warn("Unable to determine jar from url (" + locationUrl + "): " + e.getMessage());
            return Collections.emptySet();
        }

        try {
            return findResourceNamesFromJarFile(jarFile);
        } finally {
            try {
                jarFile.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private static JarFile getJarFromUrl(URL locationUrl) throws IOException {
        URLConnection con = locationUrl.openConnection();
        if (con instanceof JarURLConnection) {
            // Should usually be the case for traditional JAR files.
            JarURLConnection jarCon = (JarURLConnection) con;
            jarCon.setUseCaches(false);
            return jarCon.getJarFile();
        }
        throw new IllegalStateException("Could not open the jar file " + locationUrl);
    }

    private static Set<String> findResourceNamesFromJarFile(JarFile jarFile) {
        Set<String> resourceNames = new TreeSet<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            String entryName = entries.nextElement().getName();
            if (entryName.startsWith(DEFAULT_NATIVE_LOCATION) && !entryName.endsWith("/")) {
                LOG.debug("Discovered " + entryName);
                resourceNames.add(entryName);
            }
        }

        return resourceNames;
    }

    /**
     * Scans the classpath for resources under the configured DEFAULT_NATIVE_LOCATION.
     *
     * @return The resources that were found.
     */
    @Override
    public Collection<LoadableResource> scanForResources() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<LoadableResource> resources = new HashSet<>();
        Location defaultLocation = new Location(DEFAULT_NATIVE_LOCATION);
        for (String file : ALL_SQL_RESOURCES) {
            LOG.debug("Loading " + file);
            resources.add(new ClassPathResource(defaultLocation, file, classLoader, StandardCharsets.UTF_8));
        }
        return resources;
    }

    /**
     * Scans the classpath for concrete classes under the specified package implementing this interface.
     * Non-instantiable abstract classes are filtered out.
     *
     * @return The non-abstract classes that were found.
     */
    @Override
    public Collection<Class<?>> scanForClasses() {
        // Classes are not supported in native mode
        return Collections.emptyList();
    }
}
