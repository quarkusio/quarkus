package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import static io.quarkus.hibernate.search.orm.elasticsearch.deployment.HibernateSearchElasticsearchProcessor.HIBERNATE_SEARCH_ELASTICSEARCH;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationStaticConfiguredBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRecorder;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig;

@BuildSteps(onlyIfNot = HibernateSearchEnabled.class)
class HibernateSearchElasticsearchDisabledProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void disableHibernateSearchStaticInit(HibernateSearchElasticsearchRecorder recorder,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> staticIntegrations) {
        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            String puName = puDescriptor.getPersistenceUnitName();
            staticIntegrations.produce(new HibernateOrmIntegrationStaticConfiguredBuildItem(HIBERNATE_SEARCH_ELASTICSEARCH,
                    puName).setInitListener(recorder.createStaticInitInactiveListener()));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void disableHibernateSearchRuntimeInit(HibernateSearchElasticsearchRecorder recorder,
            HibernateSearchElasticsearchRuntimeConfig runtimeConfig,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeIntegrations) {
        Set<String> persistenceUnitNames = persistenceUnitDescriptorBuildItems.stream()
                .map(PersistenceUnitDescriptorBuildItem::getPersistenceUnitName)
                .collect(Collectors.toSet());
        recorder.checkNoExplicitActiveTrue(runtimeConfig, persistenceUnitNames);
        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            String puName = puDescriptor.getPersistenceUnitName();
            runtimeIntegrations.produce(new HibernateOrmIntegrationRuntimeConfiguredBuildItem(HIBERNATE_SEARCH_ELASTICSEARCH,
                    puName).setInitListener(recorder.createRuntimeInitInactiveListener()));
        }
    }

}
