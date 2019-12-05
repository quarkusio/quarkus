package io.quarkus.artemis.jms.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.artemis.core.deployment.ArtemisBuildTimeConfig;
import io.quarkus.artemis.core.deployment.ArtemisJmsBuildItem;
import io.quarkus.artemis.core.runtime.ArtemisRuntimeConfig;
import io.quarkus.artemis.jms.runtime.ArtemisJmsProducer;
import io.quarkus.artemis.jms.runtime.ArtemisJmsRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

public class ArtemisJmsProcessor {

    @BuildStep
    void load(BuildProducer<AdditionalBeanBuildItem> additionalBean, BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ArtemisJmsBuildItem> artemisJms) {

        artemisJms.produce(new ArtemisJmsBuildItem());
        feature.produce(new FeatureBuildItem(FeatureBuildItem.ARTEMIS_JMS));
        additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(ArtemisJmsProducer.class));
    }

    @BuildStep
    HealthBuildItem health(ArtemisBuildTimeConfig buildConfig) {
        return new HealthBuildItem(
                "io.quarkus.artemis.jms.runtime.health.ConnectionFactoryHealthCheck",
                buildConfig.healthEnabled, "artemis");
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    ArtemisJmsConfiguredBuildItem configure(ArtemisJmsRecorder recorder, ArtemisRuntimeConfig runtimeConfig,
            BeanContainerBuildItem beanContainer) {

        recorder.setConfig(runtimeConfig, beanContainer.getValue());
        return new ArtemisJmsConfiguredBuildItem();
    }
}
