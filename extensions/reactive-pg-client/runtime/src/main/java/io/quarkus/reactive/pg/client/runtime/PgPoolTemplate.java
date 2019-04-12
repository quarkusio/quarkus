/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.quarkus.reactive.pg.client.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Template;
import io.reactiverse.pgclient.PgClient;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import io.vertx.core.Vertx;

@Template
public class PgPoolTemplate {

    // Visible for testing
    static volatile PgPool pgPool;

    public RuntimeValue<PgPool> configurePgPool(RuntimeValue<Vertx> vertx, BeanContainer container,
            DataSourceConfig dataSourceConfig, PgPoolConfig pgPoolConfig, LaunchMode launchMode, ShutdownContext shutdown) {

        initialize(vertx.getValue(), dataSourceConfig, pgPoolConfig);

        PgPoolProducer producer = container.instance(PgPoolProducer.class);
        producer.initialize(pgPool);

        if (!launchMode.isDevOrTest()) {
            shutdown.addShutdownTask(this::destroy);
        }
        return new RuntimeValue<>(pgPool);
    }

    // Visible for testing
    void initialize(Vertx vertx, DataSourceConfig dataSourceConfig, PgPoolConfig pgPoolConfig) {
        if (pgPool != null) {
            return;
        }
        PgPoolOptions pgPoolOptions = toPgPoolOptions(dataSourceConfig, pgPoolConfig);
        pgPool = PgClient.pool(vertx, pgPoolOptions);
    }

    private PgPoolOptions toPgPoolOptions(DataSourceConfig dataSourceConfig, PgPoolConfig pgPoolConfig) {
        PgPoolOptions pgPoolOptions;
        if (dataSourceConfig != null) {

            pgPoolOptions = dataSourceConfig.url
                    .filter(s -> s.matches("^vertx-reactive:postgre(?:s|sql)://.*$"))
                    .map(s -> s.substring("vertx-reactive:".length()))
                    .map(PgPoolOptions::fromUri)
                    .orElse(new PgPoolOptions());

            dataSourceConfig.username.ifPresent(value -> pgPoolOptions.setUser(value));
            dataSourceConfig.password.ifPresent(value -> pgPoolOptions.setPassword(value));
            dataSourceConfig.maxSize.ifPresent(value -> pgPoolOptions.setMaxSize(value));

        } else {
            pgPoolOptions = new PgPoolOptions();
        }

        if (pgPoolConfig != null) {
            pgPoolConfig.cachePreparedStatements.ifPresent(value -> pgPoolOptions.setCachePreparedStatements(value));
            pgPoolConfig.pipeliningLimit.ifPresent(value -> pgPoolOptions.setPipeliningLimit(value));
        }

        return pgPoolOptions;
    }

    // Visible for testing
    void destroy() {
        if (pgPool != null) {
            pgPool.close();
        }
    }
}
