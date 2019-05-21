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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.internal.resource.LoadableResource;
import org.flywaydb.core.internal.resource.classpath.ClassPathResource;
import org.flywaydb.core.internal.scanner.classpath.ResourceAndClassScanner;

public final class QuarkusPathLocationScanner implements ResourceAndClassScanner {
    private static final Log LOG = LogFactory.getLog(QuarkusPathLocationScanner.class);
    /**
     * File with the migrations list. It is generated dynamically in the Flyway Quarkus Processor
     */
    public final static String MIGRATIONS_LIST_FILE = "META-INF/flyway-migrations.txt";

    /**
     * Returns the migrations loaded into the {@see MIGRATIONS_LIST_FILE}
     *
     * @return The resources that were found.
     */
    @Override
    public Collection<LoadableResource> scanForResources() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream resource = classLoader.getResourceAsStream(MIGRATIONS_LIST_FILE);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(Objects.requireNonNull(resource), StandardCharsets.UTF_8))) {
            List<String> migrations = reader.lines().collect(Collectors.toList());
            Set<LoadableResource> resources = new HashSet<>();
            for (String file : migrations) {
                LOG.debug("Loading " + file);
                resources.add(new ClassPathResource(null, file, classLoader, StandardCharsets.UTF_8));
            }
            return resources;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
