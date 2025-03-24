package io.quarkus.hibernate.envers.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.hibernate.envers.HibernateEnversBuildTimeConfig;
import io.quarkus.hibernate.envers.HibernateEnversBuildTimeConfigPersistenceUnit;
import io.quarkus.hibernate.envers.HibernateEnversRecorder;
import io.quarkus.hibernate.envers.runtime.graal.DisableLoggingFeature;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationStaticConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;

@BuildSteps(onlyIf = HibernateEnversEnabled.class)
public final class HibernateEnversProcessor {

    static final String HIBERNATE_ENVERS = "Hibernate Envers";

    @BuildStep
    List<AdditionalJpaModelBuildItem> addJpaModelClasses() {
        return Arrays.asList(
                new AdditionalJpaModelBuildItem("org.hibernate.envers.DefaultRevisionEntity"),
                new AdditionalJpaModelBuildItem("org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity"),
                new AdditionalJpaModelBuildItem("org.hibernate.envers.RevisionMapping"),
                new AdditionalJpaModelBuildItem("org.hibernate.envers.TrackingModifiedEntitiesRevisionMapping"));
    }

    @BuildStep
    public void registerEnversReflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            HibernateEnversBuildTimeConfig buildTimeConfig) {
        // This is necessary because these classes are added to the model conditionally at static init,
        // so they don't get processed by HibernateOrmProcessor and in particular don't get reflection enabled.
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "org.hibernate.envers.DefaultRevisionEntity",
                "org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity",
                "org.hibernate.envers.RevisionMapping",
                "org.hibernate.envers.TrackingModifiedEntitiesRevisionMapping")
                .reason(getClass().getName())
                .methods().build());

        List<String> classes = new ArrayList<>(buildTimeConfig.persistenceUnits().size() * 2);
        for (HibernateEnversBuildTimeConfigPersistenceUnit pu : buildTimeConfig.persistenceUnits().values()) {
            pu.revisionListener().ifPresent(classes::add);
            pu.auditStrategy().ifPresent(classes::add);
        }
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(classes.toArray(new String[0]))
                .reason("Configured Envers listeners and audit strategies")
                .methods().fields().build());
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    NativeImageFeatureBuildItem nativeImageFeature() {
        return new NativeImageFeatureBuildItem(DisableLoggingFeature.class);
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
