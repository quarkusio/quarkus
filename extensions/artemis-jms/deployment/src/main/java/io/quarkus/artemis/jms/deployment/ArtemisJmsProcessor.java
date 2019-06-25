package io.quarkus.artemis.jms.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.artemis.core.deployment.ArtemisJmsBuildItem;
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
    void load(BuildProducer<AdditionalBeanBuildItem> additionalBean, BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ArtemisJmsBuildItem> artemisJms) {

        artemisJms.produce(new ArtemisJmsBuildItem());
        feature.produce(new FeatureBuildItem(FeatureBuildItem.ARTEMIS_JMS));
        additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(ArtemisJmsProducer.class));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configure(ArtemisJmsTemplate template, ArtemisRuntimeConfig runtimeConfig,
            BeanContainerBuildItem beanContainer) {

        template.setConfig(runtimeConfig, beanContainer.getValue());
    }
}
