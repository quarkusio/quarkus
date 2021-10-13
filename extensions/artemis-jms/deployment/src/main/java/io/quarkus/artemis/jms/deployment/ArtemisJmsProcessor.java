package io.quarkus.artemis.jms.deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.jms.ConnectionFactory;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.artemis.core.deployment.ArtemisBuildTimeConfig;
import io.quarkus.artemis.core.deployment.ArtemisJmsBuildItem;
import io.quarkus.artemis.jms.runtime.ArtemisJmsRecorder;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

public class ArtemisJmsProcessor {

    @BuildStep
    void load(BuildProducer<FeatureBuildItem> feature, BuildProducer<ArtemisJmsBuildItem> artemisJms) {

        artemisJms.produce(new ArtemisJmsBuildItem());
        feature.produce(new FeatureBuildItem(Feature.ARTEMIS_JMS));
    }

    @BuildStep
    HealthBuildItem health(ArtemisBuildTimeConfig buildConfig) {
        return new HealthBuildItem(
                "io.quarkus.artemis.jms.runtime.health.ConnectionFactoryHealthCheck",
                buildConfig.healthEnabled);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    ArtemisJmsConfiguredBuildItem configure(ArtemisJmsRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer) {

        SyntheticBeanBuildItem connectionFactory = SyntheticBeanBuildItem.configure(ConnectionFactory.class)
                .supplier(recorder.getConnectionFactorySupplier())
                .scope(ApplicationScoped.class)
                .defaultBean()
                .unremovable()
                .setRuntimeInit()
                .done();
        syntheticBeanProducer.produce(connectionFactory);

        return new ArtemisJmsConfiguredBuildItem();
    }
}
