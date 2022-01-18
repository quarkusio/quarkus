package io.quarkus.hibernate.search.orm.coordination.outboxpolling;

import java.util.List;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.mapping.spi.HibernateOrmMapperOutboxPollingClasses;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.search.orm.coordination.outboxpolling.runtime.HibernateSearchOutboxPollingRecorder;
import io.quarkus.hibernate.search.orm.coordination.outboxpolling.runtime.HibernateSearchOutboxPollingRuntimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.HibernateSearchIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.HibernateSearchIntegrationStaticConfiguredBuildItem;

class HibernateSearchOutboxPollingProcessor {

    private static final String HIBERNATE_SEARCH_ORM_COORDINATION_OUTBOX_POLLING = "Hibernate Search ORM - Coordination - Outbox polling";

    @BuildStep
    void registerIndexedClasses() {
    }

    @BuildStep
    void registerInternalModel(BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<AdditionalJpaModelBuildItem> additionalJpaModel) {
        String[] avroTypes = HibernateOrmMapperOutboxPollingClasses.avroTypes().toArray(String[]::new);
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(avroTypes));
        String[] hibernateOrmTypes = HibernateOrmMapperOutboxPollingClasses.hibernateOrmTypes().toArray(String[]::new);
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, hibernateOrmTypes));
        for (String className : hibernateOrmTypes) {
            additionalJpaModel.produce(new AdditionalJpaModelBuildItem(className));
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setStaticConfig(HibernateSearchOutboxPollingRecorder recorder,
            List<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<HibernateSearchIntegrationStaticConfiguredBuildItem> staticConfigured) {
        for (HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem configuredPersistenceUnit : configuredPersistenceUnits) {
            if (!configuredPersistenceUnit.getBuildTimeConfig().coordination.strategy
                    .equals(HibernateOrmMapperOutboxPollingSettings.COORDINATION_STRATEGY_NAME)) {
                continue;
            }
            String puName = configuredPersistenceUnit.getPersistenceUnitName();
            staticConfigured.produce(new HibernateSearchIntegrationStaticConfiguredBuildItem(
                    HIBERNATE_SEARCH_ORM_COORDINATION_OUTBOX_POLLING, puName, null)
                            // Additional entities such as Agent and OutboxEvent are defined through XML
                            // (because there's no other way).
                            .setXmlMappingRequired(true));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setRuntimeConfig(HibernateSearchOutboxPollingRecorder recorder,
            HibernateSearchOutboxPollingRuntimeConfig runtimeConfig,
            List<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<HibernateSearchIntegrationRuntimeConfiguredBuildItem> runtimeConfigured) {
        for (HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem configuredPersistenceUnit : configuredPersistenceUnits) {
            if (!configuredPersistenceUnit.getBuildTimeConfig().coordination.strategy
                    .equals(HibernateOrmMapperOutboxPollingSettings.COORDINATION_STRATEGY_NAME)) {
                continue;
            }
            String puName = configuredPersistenceUnit.getPersistenceUnitName();
            runtimeConfigured.produce(new HibernateSearchIntegrationRuntimeConfiguredBuildItem(
                    HIBERNATE_SEARCH_ORM_COORDINATION_OUTBOX_POLLING, puName,
                    recorder.createRuntimeInitListener(runtimeConfig, puName)));
        }
    }

}
