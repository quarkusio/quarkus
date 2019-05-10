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

package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.Configuration;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionFullConfigTest {
    // Validation properties
    @ConfigProperty(name = "quarkus.flyway.connect-retries")
    int connectRetries;
    @ConfigProperty(name = "quarkus.flyway.schemas")
    List<String> schemaNames;
    @ConfigProperty(name = "quarkus.flyway.table")
    String tableName;
    @ConfigProperty(name = "quarkus.flyway.locations")
    List<String> locations;
    @ConfigProperty(name = "quarkus.flyway.sql-migration-prefix")
    String sqlMigrationPrefix;
    @ConfigProperty(name = "quarkus.flyway.repeatable-sql-migration-prefix")
    String repeatableSqlMigrationPrefix;

    // Quarkus built object
    @Inject
    Flyway flyway;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("full-config.properties", "application.properties"));

    @Test
    @DisplayName("Reads flyway configuration correctly")
    public void testFlywayConfigInjection() {
        Configuration configuration = flyway.getConfiguration();

        int locationsCount = locations.size();
        String joinedLocations = String.join(",", locations);
        assertEquals(locationsCount, configuration.getLocations().length);
        String configuredLocations = Arrays.stream(configuration.getLocations()).map(Location::getPath)
                .collect(Collectors.joining(","));
        assertEquals(joinedLocations, configuredLocations);

        assertEquals(sqlMigrationPrefix, configuration.getSqlMigrationPrefix());
        assertEquals(repeatableSqlMigrationPrefix, configuration.getRepeatableSqlMigrationPrefix());

        assertEquals(tableName, configuration.getTable());

        int schemasCount = schemaNames.size();
        String joinedSchemas = String.join(",", schemaNames);
        assertEquals(schemasCount, configuration.getSchemas().length);
        String configuredNames = String.join(",", configuration.getSchemas());
        assertEquals(joinedSchemas, configuredNames);

        assertEquals(connectRetries, configuration.getConnectRetries());
    }
}
