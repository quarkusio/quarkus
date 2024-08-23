package io.quarkus.hibernate.envers.deployment;

import java.util.List;
import java.util.Set;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.envers.HibernateEnversBuildTimeConfig;
import io.quarkus.hibernate.envers.HibernateEnversRecorder;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationStaticConfiguredBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;

@BuildSteps(onlyIfNot = HibernateEnversEnabled.class)
public final class HibernateEnversDisabledProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void disableHibernateEnversStaticInit(HibernateEnversRecorder recorder,
            HibernateEnversBuildTimeConfig buildTimeConfig,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> integrationProducer) {
        checkNoExplicitActiveTrue(buildTimeConfig);
        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            integrationProducer.produce(
                    new HibernateOrmIntegrationStaticConfiguredBuildItem(HibernateEnversProcessor.HIBERNATE_ENVERS,
                            puDescriptor.getPersistenceUnitName())
                            .setInitListener(recorder.createStaticInitInactiveListener())
                            // We don't need XML mapping if Envers is disabled
                            .setXmlMappingRequired(false));
        }
    }

    // TODO move this to runtime init once we implement in Hibernate ORM a way
    //  to remove entity types from the metamodel on runtime init
    public void checkNoExplicitActiveTrue(HibernateEnversBuildTimeConfig buildTimeConfig) {
        for (var entry : buildTimeConfig.persistenceUnits().entrySet()) {
            var config = entry.getValue();
            if (config.active().isPresent() && config.active().get()) {
                var puName = entry.getKey();
                String enabledPropertyKey = HibernateEnversBuildTimeConfig.extensionPropertyKey("enabled");
                String activePropertyKey = HibernateEnversBuildTimeConfig.persistenceUnitPropertyKey(puName, "active");
                throw new ConfigurationException(
                        "Hibernate Envers activated explicitly for persistence unit '" + puName
                                + "', but the Hibernate Envers extension was disabled at build time."
                                + " If you want Hibernate Envers to be active for this persistence unit, you must set '"
                                + enabledPropertyKey
                                + "' to 'true' at build time."
                                + " If you don't want Hibernate Envers to be active for this persistence unit, you must leave '"
                                + activePropertyKey
                                + "' unset or set it to 'false'.",
                        Set.of(enabledPropertyKey, activePropertyKey));
            }
        }
    }

}
