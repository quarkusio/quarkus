package io.quarkus.hibernate.envers.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.hibernate.envers.HibernateEnversBuildTimeConfig;
import io.quarkus.hibernate.envers.HibernateEnversRecorder;
import io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem;

public final class HibernateEnversProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.HIBERNATE_ENVERS);
    }

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.HIBERNATE_ENVERS);
    }

    @BuildStep
    List<AdditionalJpaModelBuildItem> addJpaModelClasses() {
        return Arrays.asList(
                new AdditionalJpaModelBuildItem(org.hibernate.envers.DefaultRevisionEntity.class),
                new AdditionalJpaModelBuildItem(org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity.class));
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
    public void applyConfig(HibernateEnversRecorder recorder,
            HibernateEnversBuildTimeConfig buildTimeConfig) {
        recorder.registerHibernateEnversIntegration(buildTimeConfig);
    }

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.envers.boot.internal.EnversServiceImpl",
                "Envers integration enabled"));
    }
}
