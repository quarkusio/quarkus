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

package io.quarkus.flyway.runtime;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import io.agroal.api.AgroalDataSource;

@ApplicationScoped
public class FlywayProducer {
    @Inject
    AgroalDataSource dataSource;
    private FlywayRuntimeConfig flywayRuntimeConfig;
    private FlywayBuildConfig flywayBuildConfig;

    @Produces
    @Dependent
    public Flyway produceFlyway() {
        FluentConfiguration configure = Flyway.configure();
        configure.dataSource(dataSource);
        flywayRuntimeConfig.connectRetries.ifPresent(configure::connectRetries);
        List<String> notEmptySchemas = filterBlanks(flywayRuntimeConfig.schemas);
        if (!notEmptySchemas.isEmpty()) {
            configure.schemas(notEmptySchemas.toArray(new String[0]));
        }
        flywayRuntimeConfig.table.ifPresent(configure::table);
        List<String> notEmptyLocations = filterBlanks(flywayBuildConfig.locations);
        if (!notEmptyLocations.isEmpty()) {
            configure.locations(notEmptyLocations.toArray(new String[0]));
        }
        flywayRuntimeConfig.sqlMigrationPrefix.ifPresent(configure::sqlMigrationPrefix);
        flywayRuntimeConfig.repeatableSqlMigrationPrefix.ifPresent(configure::repeatableSqlMigrationPrefix);
        return configure.load();
    }

    // NOTE: Have to do this filtering because SmallRye config was injecting an empty string in the list somehow!
    // TODO: remove this when https://github.com/quarkusio/quarkus/issues/2288 is fixed
    private List<String> filterBlanks(List<String> values) {
        return values.stream().filter(it -> it != null && !"".equals(it))
                .collect(Collectors.toList());
    }

    public void setFlywayRuntimeConfig(FlywayRuntimeConfig flywayRuntimeConfig) {
        this.flywayRuntimeConfig = flywayRuntimeConfig;
    }

    public void setFlywayBuildConfig(FlywayBuildConfig flywayBuildConfig) {
        this.flywayBuildConfig = flywayBuildConfig;
    }
}
