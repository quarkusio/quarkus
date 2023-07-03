package io.quarkus.kubernetes.service.binding.runtime;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;

public class KubernetesServiceBindingProcessor {
    @BuildStep
    void configFactory(BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        runTimeConfigBuilder
                .produce(new RunTimeConfigBuilderBuildItem(KubernetesServiceBindingConfigSourceFactoryBuilder.class.getName()));
    }
}
