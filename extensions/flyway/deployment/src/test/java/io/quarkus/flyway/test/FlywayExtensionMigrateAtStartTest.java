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

import javax.inject.Inject;

import org.flywaydb.core.Flyway;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionMigrateAtStartTest {
    // Quarkus built object
    @Inject
    Flyway flyway;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql")
                    .addAsResource("migrate-at-start-config.properties", "application.properties"));

    @Test
    @DisplayName("Migrates at start correctly")
    public void testFlywayConfigInjection() {
        String currentVersion = flyway.info().current().getVersion().toString();
        // Expected to be 1.0.0 as migration runs at start
        assertEquals("1.0.0", currentVersion);
    }
}
