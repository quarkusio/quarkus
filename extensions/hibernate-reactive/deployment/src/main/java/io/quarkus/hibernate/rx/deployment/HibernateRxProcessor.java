package io.quarkus.hibernate.rx.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.List;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.hibernate.orm.deployment.JpaEntitiesBuildItem;
import io.quarkus.hibernate.orm.deployment.NonJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.rx.runtime.HibernateRxRecorder;
import io.quarkus.hibernate.rx.runtime.RxSessionFactoryProducer;
import io.quarkus.hibernate.rx.runtime.RxSessionProducer;
import io.quarkus.reactive.datasource.deployment.VertxPoolBuildItem;

public final class HibernateRxProcessor {

    private static final String HIBERNATE_RX = "Hibernate Reactive";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.HIBERNATE_REACTIVE);
    }

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.HIBERNATE_REACTIVE);
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(RxSessionFactoryProducer.class)
                .addBeanClass(RxSessionProducer.class)
                .build());
    }

    @BuildStep
    void reflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        String[] classes = {
                "org.hibernate.rx.persister.entity.impl.RxSingleTableEntityPersister"
        };
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, classes));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(RecorderContext recorderContext,
            HibernateRxRecorder recorder,
            JpaEntitiesBuildItem jpaEntities,
            List<NonJpaModelBuildItem> nonJpaModels) {
        final boolean enableRx = hasEntities(jpaEntities, nonJpaModels);
        recorder.callHibernateRxFeatureInit(enableRx);
    }

    @BuildStep
    void waitForVertxPool(
            List<VertxPoolBuildItem> vertxPool,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeConfigured) {
        // Define a dependency on VertxPoolBuildItem to ensure that any Pool instances are available
        // when HibernateORM starts its persistence units
        runtimeConfigured.produce(new HibernateOrmIntegrationRuntimeConfiguredBuildItem(HIBERNATE_RX));
    }

    private boolean hasEntities(JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) {
        return !jpaEntities.getEntityClassNames().isEmpty() || !nonJpaModels.isEmpty();
    }

}
