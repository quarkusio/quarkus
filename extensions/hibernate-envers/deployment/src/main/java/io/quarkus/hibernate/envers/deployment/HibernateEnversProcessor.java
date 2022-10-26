package io.quarkus.hibernate.envers.deployment;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.hibernate.envers.HibernateEnversBuildTimeConfig;
import io.quarkus.hibernate.envers.HibernateEnversBuildTimeConfigPersistenceUnit;
import io.quarkus.hibernate.envers.HibernateEnversRecorder;
import io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationStaticConfiguredBuildItem;

@BuildSteps(onlyIf = HibernateEnversEnabled.class)
public final class HibernateEnversProcessor {

    static final String HIBERNATE_ENVERS = "Hibernate Envers";

    @BuildStep
    List<AdditionalJpaModelBuildItem> addJpaModelClasses() {
        return Arrays.asList(
                new AdditionalJpaModelBuildItem("org.hibernate.envers.DefaultRevisionEntity"),
                new AdditionalJpaModelBuildItem("org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity"));
    }

    @BuildStep
    public void registerEnversReflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            HibernateEnversBuildTimeConfig buildTimeConfig) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "org.hibernate.envers.DefaultRevisionEntity"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                "org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(false, false, "org.hibernate.tuple.entity.DynamicMapEntityTuplizer"));
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(false, false, "org.hibernate.tuple.component.DynamicMapComponentTuplizer"));

        final Map<String, HibernateEnversBuildTimeConfigPersistenceUnit> allPersistenceUnitConfigsAsMap = buildTimeConfig
                .getAllPersistenceUnitConfigsAsMap();
        if (allPersistenceUnitConfigsAsMap != null && !allPersistenceUnitConfigsAsMap.isEmpty()) {
            for (HibernateEnversBuildTimeConfigPersistenceUnit pu : allPersistenceUnitConfigsAsMap.values()) {
                pu.revisionListener.ifPresent(s -> reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, s)));
                pu.auditStrategy.ifPresent(s -> reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, s)));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void applyStaticConfig(HibernateEnversRecorder recorder, HibernateEnversBuildTimeConfig buildTimeConfig,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> integrationProducer) {
        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            String puName = puDescriptor.getPersistenceUnitName();
            integrationProducer.produce(
                    new HibernateOrmIntegrationStaticConfiguredBuildItem(HIBERNATE_ENVERS, puName)
                            .setInitListener(recorder.createStaticInitListener(buildTimeConfig, puName))
                            .setXmlMappingRequired(true));
        }
    }
}
