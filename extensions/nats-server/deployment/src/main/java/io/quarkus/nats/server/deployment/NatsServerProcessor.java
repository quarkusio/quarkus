package io.quarkus.nats.server.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.nats.server.runtime.NatsServerConfig;
import io.quarkus.nats.server.runtime.NatsServerProducer;
import io.quarkus.nats.server.runtime.NatsServerRecorder;

class NatsServerProcessor {

    private static final String FEATURE = "nats-server";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    private NatsServerConfig natsServerConfig;

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void build(
            final BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            final BuildProducer<FeatureBuildItem> featureProducer,
            final NatsServerRecorder recorder,
            final BuildProducer<BeanContainerListenerBuildItem> containerListenerProducer
    ) {

        featureProducer.produce(new FeatureBuildItem("liquibase"));

        final AdditionalBeanBuildItem unremovableProducer = AdditionalBeanBuildItem.unremovableOf(NatsServerProducer.class);
        additionalBeanProducer.produce(unremovableProducer);

        containerListenerProducer.produce(
                new BeanContainerListenerBuildItem(recorder.setLiquibaseConfig(natsServerConfig)));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void processMigration(final NatsServerRecorder recorder, final BeanContainerBuildItem beanContainer) {
        recorder.migrate(beanContainer.getValue());
    }
}
