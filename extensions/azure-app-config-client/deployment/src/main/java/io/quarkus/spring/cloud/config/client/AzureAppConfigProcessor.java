package io.quarkus.spring.cloud.config.client;

import io.quarkus.azure.app.config.client.runtime.AzureAppConfigClientConfig;
import io.quarkus.azure.app.config.client.runtime.AzureAppConfigClientRecorder;
import io.quarkus.azure.app.config.client.runtime.Response;
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

public class AzureAppConfigProcessor {

    @BuildStep
    public void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.AZURE_APP_CONFIG_CLIENT));
    }

    @BuildStep
    public void enableSsl(BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport) {
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.AZURE_APP_CONFIG_CLIENT));
    }

    @BuildStep
    public void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, Response.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, Response.Item.class));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public RunTimeConfigurationSourceValueBuildItem configure(AzureAppConfigClientRecorder recorder,
            AzureAppConfigClientConfig azureAppConfigClientConfig,
            ApplicationConfig applicationConfig) {
        return new RunTimeConfigurationSourceValueBuildItem(
                recorder.create(azureAppConfigClientConfig, applicationConfig));
    }

}
