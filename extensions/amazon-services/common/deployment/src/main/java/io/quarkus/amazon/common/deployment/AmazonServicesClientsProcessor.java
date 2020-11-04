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
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;

public class AmazonServicesClientsProcessor {
    public static final String AWS_SDK_APPLICATION_ARCHIVE_MARKERS = "software/amazon/awssdk";
    public static final String AWS_SDK_XRAY_ARCHIVE_MARKER = "com/amazonaws/xray";

    private static final String APACHE_HTTP_SERVICE = "software.amazon.awssdk.http.apache.ApacheSdkHttpService";
    private static final String NETTY_HTTP_SERVICE = "software.amazon.awssdk.http.nio.netty.NettySdkAsyncHttpService";
    private static final String URL_HTTP_SERVICE = "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService";

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

        //Register only clients that are used
        if (syncTransportNeeded) {
            if (amazonClients.stream().filter(isSyncApache).findAny().isPresent()) {
                checkClasspath(APACHE_HTTP_SERVICE, "apache-client");
                //Register Apache client as sync client
                proxyDefinition
                        .produce(new NativeImageProxyDefinitionBuildItem("org.apache.http.conn.HttpClientConnectionManager",
                                "org.apache.http.pool.ConnPoolControl",
                                "software.amazon.awssdk.http.apache.internal.conn.Wrapped"));

                serviceProvider.produce(
                        new ServiceProviderBuildItem(SdkHttpService.class.getName(), APACHE_HTTP_SERVICE));
            } else {
                checkClasspath(URL_HTTP_SERVICE, "url-connection-client");
                serviceProvider.produce(new ServiceProviderBuildItem(SdkHttpService.class.getName(), URL_HTTP_SERVICE));
            }
        }

        if (asyncTransportNeeded) {
            checkClasspath(NETTY_HTTP_SERVICE, "netty-nio-client");
            //Register netty as async client
            serviceProvider.produce(
                    new ServiceProviderBuildItem(SdkAsyncHttpService.class.getName(),
                            NETTY_HTTP_SERVICE));
        }
    }

    private void checkClasspath(String className, String dependencyName) {
        try {
            Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new DeploymentException(
                    "Missing 'software.amazon.awssdk:" + dependencyName + "' dependency on the classpath");
        }
    }
}
