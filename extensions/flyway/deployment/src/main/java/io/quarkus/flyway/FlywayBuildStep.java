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

package io.quarkus.flyway;

import static java.nio.file.Files.walk;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;

class FlywayBuildStep {

    private static final String FLYWAY_DATABASES_PATH_ROOT = "org/flywaydb/core/internal/database";
    private static final String FLYWAY_METADATA_TABLE_FILENAME = "createMetaDataTable.sql";
    private static final String[] FLYWAY_DATABASES_WITH_SQL_FILE = {
            "cockroachdb",
            "derby",
            "h2",
            "hsqldb",
            "mysql",
            "oracle",
            "postgresql",
            "redshift",
            "saphana",
            "sqlite",
            "sybasease"
    };

    private static final Logger LOGGER = Logger.getLogger(FlywayBuildStep.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.FLYWAY);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(providesCapabilities = "io.quarkus.flyway")
    void registerSubstrateResources(
            BuildProducer<SubstrateResourceBuildItem> resource,
            ApplicationArchivesBuildItem appArchives) throws IOException, URISyntaxException {
        Path root = appArchives.getRootArchive().getArchiveRoot();
        List<String> resources = generateDatabasesSQLFiles();
        resources.addAll(discoverApplicationMigrations(root));
        resource.produce(new SubstrateResourceBuildItem(resources.toArray(new String[0])));
    }

    private List<String> discoverApplicationMigrations(Path root) throws IOException, URISyntaxException {
        List<String> resources = new ArrayList<>();
        try {
            // TODO: this should be configurable, using flyway default
            String location = "db/migration";
            Enumeration<URL> migrations = Thread.currentThread().getContextClassLoader().getResources(location);
            while (migrations.hasMoreElements()) {
                URL path = migrations.nextElement();
                LOGGER.info("Adding application migrations in path: " + path);
                List<String> applicationMigrations = walk(Paths.get(path.toURI()))
                        .filter(Files::isRegularFile)
                        .map(it -> Paths.get(location, it.getFileName().toString()).toString())
                        .peek(it -> LOGGER.debug("Discovered: " + it))
                        .collect(Collectors.toList());
                resources.addAll(applicationMigrations);
            }
            return resources;
        } catch (IOException | URISyntaxException e) {
            throw e;
        }
    }

    private List<String> generateDatabasesSQLFiles() {
        List<String> result = new ArrayList<>();
        for (String database : FLYWAY_DATABASES_WITH_SQL_FILE) {
            String filePath = FLYWAY_DATABASES_PATH_ROOT + "/" + database + "/" + FLYWAY_METADATA_TABLE_FILENAME;
            result.add(filePath);
            LOGGER.debug("Adding flyway internal migration: " + filePath);
        }
        return result;
    }
}
