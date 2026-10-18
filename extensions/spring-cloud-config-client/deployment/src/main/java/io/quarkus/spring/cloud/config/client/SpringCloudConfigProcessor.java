package io.quarkus.spring.cloud.config.client;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.spring.cloud.config.client.runtime.Response;
import io.quarkus.spring.cloud.config.client.runtime.SpringCloudConfigClientConfigBuilder;

public class SpringCloudConfigProcessor {

    @BuildStep
    public void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.SPRING_CLOUD_CONFIG_CLIENT));
    }

    @BuildStep
    public void enableSsl(BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport) {
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.SPRING_CLOUD_CONFIG_CLIENT));
    }

    @BuildStep
    public void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(Response.class, Response.PropertySource.class)
                .reason(getClass().getName())
                .build());
    }

    @BuildStep
    public void springCloudConfigServer(BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        runTimeConfigBuilder
                .produce(new RunTimeConfigBuilderBuildItem(
                        SpringCloudConfigClientConfigBuilder.class.getName()));
    }
}
