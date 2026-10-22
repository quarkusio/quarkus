package io.quarkus.hibernate.orm.deployment;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.accessor.deployment.HibernateAccessorFactoryBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.HibernateOrmIntegrationStaticConfiguredBuildItem;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRecorder;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
class PropertyAccessorProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configurePropertyAccessors(HibernateOrmRecorder recorder,
            HibernateAccessorFactoryBuildItem hibernateAccessorFactory,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> integrations) {
        for (PersistenceUnitDescriptorBuildItem persistenceUnit : persistenceUnitDescriptors) {
            integrations.produce(HibernateOrmIntegrationStaticConfiguredBuildItem
                    .builder("Hibernate Accessor", persistenceUnit.getPersistenceUnitName())
                    .initListener(recorder.propertyAccessorIntegration(hibernateAccessorFactory.accessorFactory()))
                    .build());
        }
    }
}
