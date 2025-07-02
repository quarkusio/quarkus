package io.quarkus.hibernate.search.orm.outboxpolling.deployment;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.outboxpolling.mapping.spi.HibernateOrmMapperOutboxPollingClasses;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.deployment.HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.deployment.HibernateSearchEnabled;
import io.quarkus.hibernate.search.orm.elasticsearch.deployment.HibernateSearchIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.deployment.HibernateSearchIntegrationStaticConfiguredBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit;
import io.quarkus.hibernate.search.orm.outboxpolling.runtime.HibernateSearchOutboxPollingRecorder;

@BuildSteps(onlyIf = HibernateSearchEnabled.class)
class HibernateSearchOutboxPollingProcessor {

    private static final String HIBERNATE_SEARCH_ORM_COORDINATION_OUTBOX_POLLING = "Hibernate Search ORM - Outbox polling";

    @BuildStep
    void registerInternalModel(BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<AdditionalJpaModelBuildItem> additionalJpaModel) {
        String[] avroTypes = HibernateOrmMapperOutboxPollingClasses.avroTypes().toArray(String[]::new);
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(avroTypes));
        String[] hibernateOrmTypes = HibernateOrmMapperOutboxPollingClasses.hibernateOrmTypes().toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(hibernateOrmTypes)
                .reason(getClass().getName())
                .methods().fields().build());
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
            if (!isUsingOutboxPolling(configuredPersistenceUnit)) {
                continue;
            }
            String puName = configuredPersistenceUnit.getPersistenceUnitName();
            staticConfigured.produce(new HibernateSearchIntegrationStaticConfiguredBuildItem(
                    HIBERNATE_SEARCH_ORM_COORDINATION_OUTBOX_POLLING, puName,
                    recorder.createStaticInitListener(puName))
                    // Additional entities such as Agent and OutboxEvent are defined through XML
                    // (because there's no other way).
                    .setXmlMappingRequired(true));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setRuntimeConfig(HibernateSearchOutboxPollingRecorder recorder,
            List<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<HibernateSearchIntegrationRuntimeConfiguredBuildItem> runtimeConfigured) {
        for (HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem configuredPersistenceUnit : configuredPersistenceUnits) {
            if (!isUsingOutboxPolling(configuredPersistenceUnit)) {
                continue;
            }
            String puName = configuredPersistenceUnit.getPersistenceUnitName();
            runtimeConfigured.produce(new HibernateSearchIntegrationRuntimeConfiguredBuildItem(
                    HIBERNATE_SEARCH_ORM_COORDINATION_OUTBOX_POLLING, puName,
                    recorder.createRuntimeInitListener(puName)));
        }
    }

    private boolean isUsingOutboxPolling(HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem persistenceUnit) {
        HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit puConfig = persistenceUnit.getBuildTimeConfig();
        if (puConfig == null) {
            return false;
        }
        Optional<String> configuredStrategy = puConfig.coordination().strategy();
        return configuredStrategy.isPresent()
                && configuredStrategy.get().equals(HibernateOrmMapperOutboxPollingSettings.COORDINATION_STRATEGY_NAME);
    }

}
