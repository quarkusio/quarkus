package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import static io.quarkus.hibernate.search.orm.elasticsearch.deployment.HibernateSearchElasticsearchProcessor.HIBERNATE_SEARCH_ELASTICSEARCH;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.HibernateOrmIntegrationStaticConfiguredBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRecorder;

@BuildSteps(onlyIfNot = HibernateSearchEnabled.class)
class HibernateSearchElasticsearchDisabledProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void disableHibernateSearchStaticInit(HibernateSearchElasticsearchRecorder recorder,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> staticIntegrations) {
        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            String puName = puDescriptor.getPersistenceUnitName();
            staticIntegrations.produce(HibernateOrmIntegrationStaticConfiguredBuildItem.builder(HIBERNATE_SEARCH_ELASTICSEARCH,
                    puName).initListener(recorder.createStaticInitInactiveListener()).build());
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void disableHibernateSearchRuntimeInit(HibernateSearchElasticsearchRecorder recorder,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeIntegrations) {
        recorder.checkNoExplicitActiveTrue();
        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            String puName = puDescriptor.getPersistenceUnitName();
            runtimeIntegrations
                    .produce(HibernateOrmIntegrationRuntimeConfiguredBuildItem.builder(HIBERNATE_SEARCH_ELASTICSEARCH,
                            puName).initListener(recorder.createRuntimeInitInactiveListener()).build());
        }
    }

}
