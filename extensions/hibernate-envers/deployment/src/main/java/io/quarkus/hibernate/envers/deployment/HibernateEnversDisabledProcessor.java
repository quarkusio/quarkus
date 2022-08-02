package io.quarkus.hibernate.envers.deployment;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.envers.HibernateEnversRecorder;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationStaticConfiguredBuildItem;

@BuildSteps(onlyIfNot = HibernateEnversEnabled.class)
public final class HibernateEnversDisabledProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void disableHibernateEnvers(HibernateEnversRecorder recorder,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> integrationProducer) {
        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            integrationProducer.produce(
                    new HibernateOrmIntegrationStaticConfiguredBuildItem(HibernateEnversProcessor.HIBERNATE_ENVERS,
                            puDescriptor.getPersistenceUnitName())
                            .setInitListener(recorder.createStaticInitInactiveListener())
                            // We don't need XML mapping if Envers is disabled
                            .setXmlMappingRequired(false));
        }
    }
}
