package io.quarkus.dynamodb.deployment;

import javax.inject.Inject;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.JniBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.apache.ApacheSdkHttpService;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;
import software.amazon.awssdk.http.nio.netty.NettySdkAsyncHttpService;

public class DynamodbProcessor {
    public static final String AWS_SDK_APPLICATION_ARCHIVE_MARKERS = "software/amazon/awssdk";

    @Inject
    BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport;

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.DYNAMODB));
    }

    @BuildStep
    JniBuildItem jni() {
        return new JniBuildItem();
    }

    @BuildStep
    void setupDynamoDb() {
        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.DYNAMODB));
    }

    @BuildStep
    SubstrateProxyDefinitionBuildItem httpProxies() {
        return new SubstrateProxyDefinitionBuildItem("org.apache.http.conn.HttpClientConnectionManager",
                "org.apache.http.pool.ConnPoolControl", "software.amazon.awssdk.http.apache.internal.conn.Wrapped");
    }

    @BuildStep(applicationArchiveMarkers = { AWS_SDK_APPLICATION_ARCHIVE_MARKERS })
    void setupSdkAsyncHttpService(BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        //Register netty as async client
        serviceProvider.produce(
                new ServiceProviderBuildItem(SdkAsyncHttpService.class.getName(), NettySdkAsyncHttpService.class.getName()));

        //Register Apache client as sync client
        serviceProvider.produce(
                new ServiceProviderBuildItem(SdkHttpService.class.getName(), ApacheSdkHttpService.class.getName()));
    }
}
