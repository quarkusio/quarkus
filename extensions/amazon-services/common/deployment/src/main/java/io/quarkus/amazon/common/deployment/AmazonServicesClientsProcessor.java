package io.quarkus.amazon.common.deployment;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.DotName;

import com.google.common.base.Predicate;

import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig.SyncClientType;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;

public class AmazonServicesClientsProcessor {
    public static final String AWS_SDK_APPLICATION_ARCHIVE_MARKERS = "software/amazon/awssdk";
    public static final String AWS_SDK_XRAY_ARCHIVE_MARKER = "com/amazonaws/xray";

    private static final DotName EXECUTION_INTERCEPTOR_NAME = DotName.createSimple(ExecutionInterceptor.class.getName());

    @BuildStep
    void globalInterceptors(BuildProducer<AmazonClientInterceptorsPathBuildItem> producer) {
        producer.produce(
                new AmazonClientInterceptorsPathBuildItem("software/amazon/awssdk/global/handlers/execution.interceptors"));
    }

    @BuildStep
    void awsAppArchiveMarkers(BuildProducer<AdditionalApplicationArchiveMarkerBuildItem> archiveMarker) {
        archiveMarker.produce(new AdditionalApplicationArchiveMarkerBuildItem(AWS_SDK_APPLICATION_ARCHIVE_MARKERS));
        archiveMarker.produce(new AdditionalApplicationArchiveMarkerBuildItem(AWS_SDK_XRAY_ARCHIVE_MARKER));
    }

    @BuildStep
    void runtimeInitialize(BuildProducer<RuntimeInitializedClassBuildItem> producer) {
        // FullJitterBackoffStrategy uses j.u.Ramdom, so needs to be runtime-initialized
        producer.produce(
                new RuntimeInitializedClassBuildItem("software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy"));
    }

    @BuildStep
    void setup(CombinedIndexBuildItem combinedIndexBuildItem,
            List<AmazonClientBuildItem> amazonClients,
            List<AmazonClientInterceptorsPathBuildItem> interceptors,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinition,
            BuildProducer<ServiceProviderBuildItem> serviceProvider) {

        interceptors.stream().map(AmazonClientInterceptorsPathBuildItem::getInterceptorsPath)
                .forEach(path -> resource.produce(new NativeImageResourceBuildItem(path)));

        //Discover all interceptor implementations
        List<String> knownInterceptorImpls = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(EXECUTION_INTERCEPTOR_NAME)
                .stream()
                .map(c -> c.name().toString()).collect(Collectors.toList());

        //Validate configurations
        for (AmazonClientBuildItem client : amazonClients) {
            SdkBuildTimeConfig clientSdkConfig = client.getBuildTimeSdkConfig();
            if (clientSdkConfig != null) {
                clientSdkConfig.interceptors.orElse(Collections.emptyList()).forEach(interceptorClassName -> {
                    interceptorClassName = interceptorClassName.trim();
                    if (!knownInterceptorImpls.contains(interceptorClassName)) {
                        throw new ConfigurationError(
                                String.format(
                                        "quarkus.%s.interceptors (%s) - must list only existing implementations of software.amazon.awssdk.core.interceptor.ExecutionInterceptor",
                                        client.getAwsClientName(),
                                        clientSdkConfig.interceptors.toString()));
                    }
                });
            }
        }

        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false,
                knownInterceptorImpls.toArray(new String[knownInterceptorImpls.size()])));

        reflectiveClasses
                .produce(new ReflectiveClassBuildItem(true, false, "com.sun.xml.internal.stream.XMLInputFactoryImpl"));
        reflectiveClasses
                .produce(new ReflectiveClassBuildItem(true, false, "com.sun.xml.internal.stream.XMLOutputFactoryImpl"));

        boolean syncTransportNeeded = amazonClients.stream().anyMatch(item -> item.getSyncClassName().isPresent());
        boolean asyncTransportNeeded = amazonClients.stream().anyMatch(item -> item.getAsyncClassName().isPresent());
        final Predicate<AmazonClientBuildItem> isSyncApache = client -> client
                .getBuildTimeSyncConfig().type == SyncClientType.APACHE;

        // Register what's needed depending on the clients in the classpath and the configuration.
        // We use the configuration to guide us but if we don't have any clients configured,
        // we still register what's needed depending on what is in the classpath.
        boolean isSyncApacheInClasspath = isInClasspath(AmazonHttpClients.APACHE_HTTP_SERVICE);
        boolean isSyncUrlConnectionInClasspath = isInClasspath(AmazonHttpClients.URL_CONNECTION_HTTP_SERVICE);
        boolean isAsyncInClasspath = isInClasspath(AmazonHttpClients.NETTY_HTTP_SERVICE);

        // Check that the clients required by the configuration are available
        if (syncTransportNeeded) {
            if (amazonClients.stream().filter(isSyncApache).findAny().isPresent()) {
                if (isSyncApacheInClasspath) {
                    registerSyncApacheClient(proxyDefinition, serviceProvider);
                } else {
                    throw missingDependencyException("apache-client");
                }
            } else if (isSyncUrlConnectionInClasspath) {
                registerSyncUrlConnectionClient(serviceProvider);
            } else {
                throw missingDependencyException("url-connection-client");
            }
        } else {
            // even if we don't register any clients via configuration, we still register the clients
            // but this time only based on the classpath.
            if (isSyncApacheInClasspath) {
                registerSyncApacheClient(proxyDefinition, serviceProvider);
            } else if (isSyncUrlConnectionInClasspath) {
                registerSyncUrlConnectionClient(serviceProvider);
            }
        }

        if (isAsyncInClasspath) {
            registerAsyncNettyClient(serviceProvider);
        } else if (asyncTransportNeeded) {
            throw missingDependencyException("netty-nio-client");
        }
    }

    private static void registerSyncApacheClient(BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinition,
            BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        proxyDefinition
                .produce(new NativeImageProxyDefinitionBuildItem("org.apache.http.conn.HttpClientConnectionManager",
                        "org.apache.http.pool.ConnPoolControl",
                        "software.amazon.awssdk.http.apache.internal.conn.Wrapped"));

        serviceProvider.produce(
                new ServiceProviderBuildItem(SdkHttpService.class.getName(), AmazonHttpClients.APACHE_HTTP_SERVICE));
    }

    private static void registerSyncUrlConnectionClient(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(
                new ServiceProviderBuildItem(SdkHttpService.class.getName(), AmazonHttpClients.URL_CONNECTION_HTTP_SERVICE));
    }

    private static void registerAsyncNettyClient(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(
                new ServiceProviderBuildItem(SdkAsyncHttpService.class.getName(),
                        AmazonHttpClients.NETTY_HTTP_SERVICE));
    }

    private static boolean isInClasspath(String className) {
        try {
            Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private DeploymentException missingDependencyException(String dependencyName) {
        return new DeploymentException("Missing 'software.amazon.awssdk:" + dependencyName + "' dependency on the classpath");
    }
}
