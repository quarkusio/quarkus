package io.quarkus.hibernate.envers.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.hibernate.envers.HibernateEnversBuildTimeConfig;
import io.quarkus.hibernate.envers.HibernateEnversRecorder;
import io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationStaticConfiguredBuildItem;

public final class HibernateEnversProcessor {

    private static final String HIBERNATE_ENVERS = "Hibernate Envers";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.HIBERNATE_ENVERS);
    }

    @BuildStep
    List<AdditionalJpaModelBuildItem> addJpaModelClasses() {
        return Arrays.asList(
                new AdditionalJpaModelBuildItem("org.hibernate.envers.DefaultRevisionEntity"),
                new AdditionalJpaModelBuildItem("org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity"));
    }

    @BuildStep
    public void registerEnversReflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "org.hibernate.envers.DefaultRevisionEntity"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                "org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(false, false, "org.hibernate.tuple.entity.DynamicMapEntityTuplizer"));
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(false, false, "org.hibernate.tuple.component.DynamicMapComponentTuplizer"));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void applyConfig(HibernateEnversRecorder recorder, HibernateEnversBuildTimeConfig buildTimeConfig,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> integrationProducer) {
        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            integrationProducer.produce(
                    new HibernateOrmIntegrationStaticConfiguredBuildItem(HIBERNATE_ENVERS,
                            puDescriptor.getPersistenceUnitName())
                                    .setInitListener(recorder.createStaticInitListener(buildTimeConfig))
                                    .setXmlMappingRequired(true));
        }
    }

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.envers.boot.internal.EnversServiceImpl",
                "Envers integration enabled"));
    }
}
