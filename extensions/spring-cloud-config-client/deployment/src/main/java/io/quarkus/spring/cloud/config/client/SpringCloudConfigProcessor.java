package io.quarkus.spring.cloud.config.client;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceValueBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.spring.cloud.config.client.runtime.Response;
import io.quarkus.spring.cloud.config.client.runtime.SpringCloudConfigClientConfig;
import io.quarkus.spring.cloud.config.client.runtime.SpringCloudConfigClientRecorder;

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
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, Response.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, Response.PropertySource.class));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public RunTimeConfigurationSourceValueBuildItem configure(SpringCloudConfigClientRecorder recorder,
            SpringCloudConfigClientConfig springCloudConfigClientConfig,
            ApplicationConfig applicationConfig,
            TlsConfig tlsConfig) {
        return new RunTimeConfigurationSourceValueBuildItem(
                recorder.create(springCloudConfigClientConfig, applicationConfig, tlsConfig));
    }

}
