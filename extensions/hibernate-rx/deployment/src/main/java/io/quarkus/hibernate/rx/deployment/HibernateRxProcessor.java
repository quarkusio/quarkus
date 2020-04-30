package io.quarkus.hibernate.rx.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.List;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.hibernate.orm.deployment.JpaEntitiesBuildItem;
import io.quarkus.hibernate.orm.deployment.NonJpaModelBuildItem;
import io.quarkus.hibernate.rx.runtime.HibernateRxRecorder;

public final class HibernateRxProcessor {

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
    @Record(STATIC_INIT)
    public void build(RecorderContext recorderContext,
            HibernateRxRecorder recorder,
            JpaEntitiesBuildItem jpaEntities,
            List<NonJpaModelBuildItem> nonJpaModels) {
        System.out.println("@AGG in HibernateRxProcessor.build()");

        final boolean enableRx = hasEntities(jpaEntities, nonJpaModels);
        recorder.callHibernateRxFeatureInit(enableRx);
    }

    private boolean hasEntities(JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) {
        return !jpaEntities.getEntityClassNames().isEmpty() || !nonJpaModels.isEmpty();
    }

}
