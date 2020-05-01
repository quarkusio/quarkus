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
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.hibernate.orm.deployment.JpaEntitiesBuildItem;
import io.quarkus.hibernate.orm.deployment.NonJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.rx.runtime.HibernateRxRecorder;
import io.quarkus.hibernate.rx.runtime.RxSessionFactoryProducer;
import io.quarkus.reactive.pg.client.deployment.PgPoolBuildItem;

public final class HibernateRxProcessor {

    private static final String HIBERNATE_RX = "Hibernate RX";

    @BuildStep
    FeatureBuildItem feature() {
        System.out.println("@AGG in HibernateRxProcessor.feature()");
        return new FeatureBuildItem(FeatureBuildItem.HIBERNATE_RX);
    }

    @BuildStep
    CapabilityBuildItem capability() {
        System.out.println("@AGG in HibernateRxProcessor.capability()");
        return new CapabilityBuildItem(Capabilities.HIBERNATE_RX);
    }

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(RxSessionFactoryProducer.class);
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(RecorderContext recorderContext,
            HibernateRxRecorder recorder,
            JpaEntitiesBuildItem jpaEntities,
            List<NonJpaModelBuildItem> nonJpaModels) {
        System.out.println("@AGG in HibernateRxProcessor.build()");

        final boolean enableRx = hasEntities(jpaEntities, nonJpaModels);
        recorder.callHibernateRxFeatureInit(enableRx);
    }

    @BuildStep
    void waitForVertxPool(
            PgPoolBuildItem pgPool, // TODO @AGG: make this a generic pool build item so we don't need to depend on an impl
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeConfigured) {
        // Define a dependency on PgPoolBuildItem to ensure that any Pool instances are available
        // when HibernateORM starts its persistence units
        System.out.println("@AGG HibernateRX processor is configured with pool=" + pgPool.getPgPool());
        runtimeConfigured.produce(new HibernateOrmIntegrationRuntimeConfiguredBuildItem(HIBERNATE_RX));
    }

    private boolean hasEntities(JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) {
        return !jpaEntities.getEntityClassNames().isEmpty() || !nonJpaModels.isEmpty();
    }

}
