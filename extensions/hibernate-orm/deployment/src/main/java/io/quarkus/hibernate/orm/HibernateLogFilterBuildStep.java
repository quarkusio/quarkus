/*
 * Copyright 2018 Red Hat, Inc.
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

package io.quarkus.hibernate.orm;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;

/**
 * Processor that sets up log filters for Hibernate
 */
public final class HibernateLogFilterBuildStep {

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.Version", "HHH000412"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.cfg.Environment", "HHH000206"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.bytecode.enhance.spi.Enhancer", "Enhancing [%s] as"));
        filters.produce(new LogCleanupFilterBuildItem(
                "org.hibernate.bytecode.enhance.internal.bytebuddy.BiDirectionalAssociationHandler", "Could not find"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.jpa.internal.util.LogHelper", "HHH000204"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.annotations.common.Version", "HCANN000001"));
        filters.produce(
                new LogCleanupFilterBuildItem("org.hibernate.engine.jdbc.env.internal.LobCreatorBuilderImpl", "HHH000422"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.dialect.Dialect", "HHH000400"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.type.BasicTypeRegistry", "HHH000270"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.orm.beans", "HHH10005002"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.tuple.PojoInstantiator", "HHH000182"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.tuple.entity.EntityMetamodel", "HHH000157"));
        filters.produce(new LogCleanupFilterBuildItem(
                "org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformInitiator", "HHH000490"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.tool.schema.internal.SchemaCreatorImpl", "HHH000476"));
        filters.produce(
                new LogCleanupFilterBuildItem("org.hibernate.hql.internal.QueryTranslatorFactoryInitiator", "HHH000397"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.jpa.boot.internal.PersistenceXmlParser", "HHH000318"));
    }
}
