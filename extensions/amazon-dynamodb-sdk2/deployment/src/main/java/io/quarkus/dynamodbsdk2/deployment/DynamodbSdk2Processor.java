package io.quarkus.dynamodbsdk2.deployment;

import javax.inject.Inject;

import org.apache.commons.logging.impl.Jdk14Logger;
import org.apache.commons.logging.impl.LogFactoryImpl;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.apache.ApacheSdkHttpService;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;
import software.amazon.awssdk.http.nio.netty.NettySdkAsyncHttpService;

public class DynamodbSdk2Processor {
    public static final String AWS_DYNAMODB_SDK2_APPLICATION_ARCHIVE_MARKERS = "software/amazon";

    @Inject
    BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport;

    @BuildStep
    public void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.DYNAMODB_SDK2));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupDynamoDb() {
        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.DYNAMODB_SDK2));
    }

    @BuildStep
    SubstrateProxyDefinitionBuildItem httpProxies() {
        return new SubstrateProxyDefinitionBuildItem("org.apache.http.conn.HttpClientConnectionManager",
                "org.apache.http.pool.ConnPoolControl", "software.amazon.awssdk.http.apache.internal.conn.Wrapped");
    }

    @BuildStep(applicationArchiveMarkers = { AWS_DYNAMODB_SDK2_APPLICATION_ARCHIVE_MARKERS })
    void setupSdkAsyncHttpService(BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        //Register netty as async client
        serviceProvider.produce(
                new ServiceProviderBuildItem(SdkAsyncHttpService.class.getName(), NettySdkAsyncHttpService.class.getName()));

        //Register Apache client as sync client
        serviceProvider.produce(
                new ServiceProviderBuildItem(SdkHttpService.class.getName(), ApacheSdkHttpService.class.getName()));

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                LogFactoryImpl.class.getName(),
                Jdk14Logger.class.getName()));
    }
}
