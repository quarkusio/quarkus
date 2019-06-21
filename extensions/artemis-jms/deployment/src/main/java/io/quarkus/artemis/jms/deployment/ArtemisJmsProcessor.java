package io.quarkus.artemis.jms.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.artemis.core.runtime.ArtemisBuildConfig;
import io.quarkus.artemis.core.runtime.ArtemisRuntimeConfig;
import io.quarkus.artemis.jms.runtime.ArtemisJmsProducer;
import io.quarkus.artemis.jms.runtime.ArtemisJmsTemplate;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class ArtemisJmsProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void load(ArtemisBuildConfig config, BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<FeatureBuildItem> feature) {

        if (config.getProtocol() != ArtemisBuildConfig.Protocol.JMS) {
            return;
        }
        feature.produce(new FeatureBuildItem(FeatureBuildItem.ARTEMIS_JMS));
        additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(ArtemisJmsProducer.class));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configure(ArtemisBuildConfig buildConfig, ArtemisJmsTemplate template, ArtemisRuntimeConfig runtimeConfig,
            BeanContainerBuildItem beanContainer) {

        if (buildConfig.getProtocol() == ArtemisBuildConfig.Protocol.JMS) {
            template.setConfig(runtimeConfig, beanContainer.getValue());
        }
    }
}
