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

package io.quarkus.reactive.pg.client.deployment;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.reactive.pg.client.runtime.DataSourceConfig;
import io.quarkus.reactive.pg.client.runtime.PgPoolConfig;
import io.quarkus.reactive.pg.client.runtime.PgPoolProducer;
import io.quarkus.reactive.pg.client.runtime.PgPoolTemplate;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.reactiverse.pgclient.PgPool;

class ReactivePgClientProcessor {

    private static final Logger LOGGER = Logger.getLogger(ReactivePgClientProcessor.class.getName());

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(PgPoolProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    PgPoolBuildItem build(BuildProducer<FeatureBuildItem> feature, PgPoolTemplate template, VertxBuildItem vertx,
            BeanContainerBuildItem beanContainer, LaunchModeBuildItem launchMode, ShutdownContextBuildItem shutdown,
            DataSourceConfig dataSourceConfig, PgPoolConfig pgPoolConfig) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.REACTIVE_PG_CLIENT));

        RuntimeValue<PgPool> pgPool = template.configurePgPool(vertx.getVertx(), beanContainer.getValue(), dataSourceConfig,
                pgPoolConfig, launchMode.getLaunchMode(), shutdown);

        return new PgPoolBuildItem(pgPool);
    }
}
