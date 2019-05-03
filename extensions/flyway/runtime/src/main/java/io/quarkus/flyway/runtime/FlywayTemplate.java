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

import org.flywaydb.core.Flyway;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Template;

@Template
public class FlywayTemplate {

    public BeanContainerListener setFlywayBuildConfig(FlywayBuildConfig flywayBuildConfig) {
        return beanContainer -> {
            FlywayProducer producer = beanContainer.instance(FlywayProducer.class);
            producer.setFlywayBuildConfig(flywayBuildConfig);
        };
    }

    public void configureFlywayProperties(FlywayRuntimeConfig flywayRuntimeConfig, BeanContainer container) {
        container.instance(FlywayProducer.class).setFlywayRuntimeConfig(flywayRuntimeConfig);
    }

    public void doStartActions(FlywayRuntimeConfig config, BeanContainer container) {
        if (config.migrateAtStart) {
            Flyway flyway = container.instance(Flyway.class);
            flyway.migrate();
        }
    }
}
