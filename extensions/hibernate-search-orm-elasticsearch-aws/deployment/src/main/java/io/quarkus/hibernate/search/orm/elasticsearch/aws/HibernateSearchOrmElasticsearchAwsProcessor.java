package io.quarkus.hibernate.search.orm.elasticsearch.aws;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.aws.runtime.HibernateSearchOrmElasticsearchAwsRecorder;
import io.quarkus.hibernate.search.orm.elasticsearch.aws.runtime.HibernateSearchOrmElasticsearchAwsRuntimeConfig;

class HibernateSearchOrmElasticsearchAwsProcessor {

    private static final String HIBERNATE_SEARCH_ORM_ELASTICSEARCH_AWS = "Hibernate Search ORM + Elasticsearch - AWS Integration";

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setRuntimeConfig(HibernateSearchOrmElasticsearchAwsRecorder recorder,
            HibernateSearchOrmElasticsearchAwsRuntimeConfig runtimeConfig,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeConfigured) {
        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            runtimeConfigured.produce(new HibernateOrmIntegrationRuntimeConfiguredBuildItem(
                    HIBERNATE_SEARCH_ORM_ELASTICSEARCH_AWS,
                    puDescriptor.getPersistenceUnitName())
                            .setInitListener(
                                    recorder.createRuntimeInitListener(runtimeConfig, puDescriptor.getPersistenceUnitName())));
        }
    }

}
