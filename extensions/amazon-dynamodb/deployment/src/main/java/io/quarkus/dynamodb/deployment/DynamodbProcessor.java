package io.quarkus.dynamodb.deployment;

import java.net.URI;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.JniBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.dynamodb.runtime.*;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.apache.ApacheSdkHttpService;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.utils.StringUtils;

public class DynamodbProcessor {
    public static final String AWS_SDK_APPLICATION_ARCHIVE_MARKERS = "software/amazon/awssdk";

    private static final DotName SYNC_CLIENT_NAME = DotName.createSimple(DynamoDbClient.class.getName());
    private static final DotName ASYNC_CLIENT_NAME = DotName.createSimple(DynamoDbAsyncClient.class.getName());

    DynamodbConfig config;

    @BuildStep
    JniBuildItem jni() {
        return new JniBuildItem();
    }

    @BuildStep(applicationArchiveMarkers = { AWS_SDK_APPLICATION_ARCHIVE_MARKERS })
    void setup(BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.DYNAMODB));

        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.DYNAMODB));

        checkConfig(config);

        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(DynamodbClientProducer.class));
    }

    @BuildStep
    DynamodbClientBuildItem analyzeDynamodbClientInjectionPoints(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<SubstrateProxyDefinitionBuildItem> proxyDefinition) {

        boolean createSyncClient = false;
        boolean createAsyncClient = false;

        for (InjectionPointInfo injectionPoint : beanRegistrationPhase.getContext()
                .get(BuildExtension.Key.INJECTION_POINTS)) {
            Type requiredType = injectionPoint.getRequiredType();

            if (SYNC_CLIENT_NAME.equals(requiredType.name())) {
                createSyncClient = true;
            }

            // Until we have a compatible version of the AWS SDK with the Netty version used in Quarkus, disable the
            // async support.
            if (ASYNC_CLIENT_NAME.equals(requiredType.name())) {
                throw new UnsupportedOperationException("Async client not supported");
                //                createAsyncClient = true;
            }
        }

        if (createSyncClient) {
            //Register Apache client as sync client
            proxyDefinition
                    .produce(new SubstrateProxyDefinitionBuildItem("org.apache.http.conn.HttpClientConnectionManager",
                            "org.apache.http.pool.ConnPoolControl",
                            "software.amazon.awssdk.http.apache.internal.conn.Wrapped"));

            serviceProvider.produce(
                    new ServiceProviderBuildItem(SdkHttpService.class.getName(), ApacheSdkHttpService.class.getName()));
        }

        if (createAsyncClient) {
            //Register netty as async client
            // Until we have a compatible version of the AWS SDK with the Netty version used in Quarkus, disable the
            // async support.
            //            serviceProvider.produce(
            //                    new ServiceProviderBuildItem(SdkAsyncHttpService.class.getName(),
            //                            NettySdkAsyncHttpService.class.getName()));
        }

        return new DynamodbClientBuildItem(createSyncClient, createAsyncClient);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void buildClients(DynamodbClientBuildItem clientBuildItem, DynamodbRecorder recorder,
            BeanContainerBuildItem beanContainer, ShutdownContextBuildItem shutdown) {

        recorder.configureRuntimeConfig(config);

        if (clientBuildItem.isCreateSyncClient()) {
            recorder.createClient(beanContainer.getValue(), shutdown);
        }

        if (clientBuildItem.isCreateAsyncClient()) {
            recorder.createAsyncClient(beanContainer.getValue(), shutdown);
        }
    }

    private static void checkConfig(DynamodbConfig config) {
        if (config.endpointOverride.isPresent()) {
            URI endpointOverride = config.endpointOverride.get();
            if (StringUtils.isBlank(endpointOverride.getScheme())) {
                throw new ConfigurationError(
                        String.format("quarkus.dynamodb.endpoint-override (%s) - scheme must be specified",
                                endpointOverride.toString()));
            }
        }

        if (config.credentials.type == AwsCredentialsProviderType.STATIC) {
            if (StringUtils.isBlank(config.credentials.staticProvider.accessKeyId)
                    || StringUtils.isBlank(config.credentials.staticProvider.secretAccessKey)) {
                throw new ConfigurationError(
                        "quarkus.dynamodb.credentials.static-provider.access-key-id and "
                                + "quarkus.dynamodb.credentials.static-provider.secret-access-key cannot be empty if STATIC credentials provider used.");
            }
        }
        if (config.credentials.type == AwsCredentialsProviderType.PROCESS) {
            if (StringUtils.isBlank(config.credentials.processProvider.command)) {
                throw new ConfigurationError(
                        "quarkus.dynamodb.credentials.process-provider.command cannot be empty if PROCESS credentials provider used.");
            }
        }
        if (config.syncClient != null) {
            checkSyncClientConfig(config.syncClient);
        }
        if (config.asyncClient != null) {
            checkAsyncClientConfig(config.asyncClient);
        }
    }

    private static void checkSyncClientConfig(AwsApacheHttpClientConfig syncClient) {
        if (syncClient.maxConnections.isPresent() && syncClient.maxConnections.getAsInt() <= 0) {
            throw new ConfigurationError("quarkus.dynamodb.sync-client.max-connections may not be negative or zero.");
        }
        if (syncClient.proxy != null && syncClient.proxy.enabled) {
            URI proxyEndpoint = syncClient.proxy.endpoint;
            if (proxyEndpoint != null) {
                if (StringUtils.isBlank(proxyEndpoint.getScheme())) {
                    throw new ConfigurationError(
                            String.format("quarkus.dynamodb.sync-client.proxy.endpoint (%s) - scheme must be specified",
                                    proxyEndpoint.toString()));
                }
                if (StringUtils.isNotBlank(proxyEndpoint.getUserInfo())) {
                    throw new ConfigurationError(
                            String.format(
                                    "quarkus.dynamodb.sync-client.proxy.endpoint (%s) - user info is not supported.",
                                    proxyEndpoint.toString()));
                }
                if (StringUtils.isNotBlank(proxyEndpoint.getPath())) {
                    throw new ConfigurationError(
                            String.format("quarkus.dynamodb.sync-client.proxy.endpoint (%s) - path is not supported.",
                                    proxyEndpoint.toString()));
                }
                if (StringUtils.isNotBlank(proxyEndpoint.getQuery())) {
                    throw new ConfigurationError(
                            String.format("quarkus.dynamodb.sync-client.proxy.endpoint (%s) - query is not supported.",
                                    proxyEndpoint.toString()));
                }
                if (StringUtils.isNotBlank(proxyEndpoint.getFragment())) {
                    throw new ConfigurationError(
                            String.format(
                                    "quarkus.dynamodb.sync-client.proxy.endpoint (%s) - fragment is not supported.",
                                    proxyEndpoint.toString()));
                }
            }
        }
    }

    private static void checkAsyncClientConfig(AwsNettyNioAsyncHttpClientConfig asyncClient) {
        if (asyncClient.maxConcurrency.isPresent() && asyncClient.maxConcurrency.get() <= 0) {
            throw new ConfigurationError("quarkus.dynamodb.async-client.max-concurrency may not be negative or zero.");
        }
        if (asyncClient.maxHttp2Streams.isPresent() && asyncClient.maxHttp2Streams.get() <= 0) {
            throw new ConfigurationError(
                    "quarkus.dynamodb.async-client.max-http2-streams may not be negative or zero.");
        }
        if (asyncClient.maxPendingConnectionAcquires.isPresent()
                && asyncClient.maxPendingConnectionAcquires.get() <= 0) {
            throw new ConfigurationError(
                    "quarkus.dynamodb.async-client.max-pending-connection-acquires may not be negative or zero.");
        }
        if (asyncClient.eventLoop.override) {
            if (asyncClient.eventLoop.numberOfThreads <= 0) {
                throw new ConfigurationError(
                        "quarkus.dynamodb.async-client.event-loop.number-of-threads may not be negative or zero.");
            }
        }
    }
}
