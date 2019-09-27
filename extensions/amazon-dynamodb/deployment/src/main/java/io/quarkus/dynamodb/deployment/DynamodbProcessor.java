package io.quarkus.dynamodb.deployment;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.JniBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.dynamodb.runtime.ApacheHttpClientConfig;
import io.quarkus.dynamodb.runtime.AwsCredentialsProviderType;
import io.quarkus.dynamodb.runtime.DynamodbClientProducer;
import io.quarkus.dynamodb.runtime.DynamodbConfig;
import io.quarkus.dynamodb.runtime.DynamodbRecorder;
import io.quarkus.dynamodb.runtime.NettyHttpClientConfig;
import io.quarkus.dynamodb.runtime.TlsManagersProviderConfig;
import io.quarkus.dynamodb.runtime.TlsManagersProviderType;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.apache.ApacheSdkHttpService;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;
import software.amazon.awssdk.http.nio.netty.NettySdkAsyncHttpService;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.utils.StringUtils;

public class DynamodbProcessor {
    public static final String AWS_SDK_APPLICATION_ARCHIVE_MARKERS = "software/amazon/awssdk";

    private static final List<String> INTERCEPTOR_PATHS = Arrays.asList(
            "software/amazon/awssdk/global/handlers/execution.interceptors",
            "software/amazon/awssdk/services/dynamodb/execution.interceptors");

    private static final DotName EXECUTION_INTERCEPTOR_NAME = DotName.createSimple(ExecutionInterceptor.class.getName());
    private static final DotName SYNC_CLIENT_NAME = DotName.createSimple(DynamoDbClient.class.getName());
    private static final DotName ASYNC_CLIENT_NAME = DotName.createSimple(DynamoDbAsyncClient.class.getName());

    DynamodbConfig config;

    @BuildStep
    JniBuildItem jni() {
        return new JniBuildItem();
    }

    @BuildStep(applicationArchiveMarkers = { AWS_SDK_APPLICATION_ARCHIVE_MARKERS })
    void setup(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<SubstrateResourceBuildItem> resource) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.DYNAMODB));

        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.DYNAMODB));

        INTERCEPTOR_PATHS.stream().forEach(path -> resource.produce(new SubstrateResourceBuildItem(path)));

        List<String> knownInterceptorImpls = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(EXECUTION_INTERCEPTOR_NAME)
                .stream()
                .map(c -> c.name().toString()).collect(Collectors.toList());

        checkConfig(config, knownInterceptorImpls);

        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false,
                knownInterceptorImpls.toArray(new String[knownInterceptorImpls.size()])));

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

            if (ASYNC_CLIENT_NAME.equals(requiredType.name())) {
                createAsyncClient = true;
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
            serviceProvider.produce(
                    new ServiceProviderBuildItem(SdkAsyncHttpService.class.getName(),
                            NettySdkAsyncHttpService.class.getName()));
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

    private static void checkConfig(DynamodbConfig config, List<String> knownInterceptorImpls) {
        if (config.sdk != null) {
            if (config.sdk.endpointOverride.isPresent()) {
                URI endpointOverride = config.sdk.endpointOverride.get();
                if (StringUtils.isBlank(endpointOverride.getScheme())) {
                    throw new ConfigurationError(
                            String.format("quarkus.dynamodb.sdk.endpoint-override (%s) - scheme must be specified",
                                    endpointOverride.toString()));
                }
            }
            config.sdk.interceptors.stream().forEach(interceptorClass -> {
                if (!knownInterceptorImpls.contains(interceptorClass.getName())) {
                    throw new ConfigurationError(
                            String.format(
                                    "quarkus.dynamodb.sdk.interceptors (%s) - must list only existing implementations of software.amazon.awssdk.core.interceptor.ExecutionInterceptor",
                                    config.sdk.interceptors.toString()));
                }
            });
        }

        if (config.aws != null) {
            if (config.aws.credentials.type == AwsCredentialsProviderType.STATIC) {
                if (StringUtils.isBlank(config.aws.credentials.staticProvider.accessKeyId)
                        || StringUtils.isBlank(config.aws.credentials.staticProvider.secretAccessKey)) {
                    throw new ConfigurationError(
                            "quarkus.dynamodb.aws.credentials.static-provider.access-key-id and "
                                    + "quarkus.dynamodb.aws.credentials.static-provider.secret-access-key cannot be empty if STATIC credentials provider used.");
                }
            }
            if (config.aws.credentials.type == AwsCredentialsProviderType.PROCESS) {
                if (StringUtils.isBlank(config.aws.credentials.processProvider.command)) {
                    throw new ConfigurationError(
                            "quarkus.dynamodb.aws.credentials.process-provider.command cannot be empty if PROCESS credentials provider used.");
                }
            }
        }

        if (config.syncClient != null) {
            checkSyncClientConfig(config.syncClient);
        }
        if (config.asyncClient != null) {
            checkAsyncClientConfig(config.asyncClient);
        }
    }

    private static void checkSyncClientConfig(ApacheHttpClientConfig syncClient) {
        if (syncClient.maxConnections <= 0) {
            throw new ConfigurationError("quarkus.dynamodb.sync-client.max-connections may not be negative or zero.");
        }
        if (syncClient.proxy != null && syncClient.proxy.enabled) {
            URI proxyEndpoint = syncClient.proxy.endpoint;
            if (proxyEndpoint != null) {
                validateProxyEndpoint(proxyEndpoint, "sync");
            }
        }
        validateTlsManagersProvider(syncClient.tlsManagersProvider, "sync");
    }

    private static void checkAsyncClientConfig(NettyHttpClientConfig asyncClient) {
        if (asyncClient.maxConcurrency <= 0) {
            throw new ConfigurationError("quarkus.dynamodb.async-client.max-concurrency may not be negative or zero.");
        }
        if (asyncClient.maxHttp2Streams < 0) {
            throw new ConfigurationError(
                    "quarkus.dynamodb.async-client.max-http2-streams may not be negative.");
        }
        if (asyncClient.maxPendingConnectionAcquires <= 0) {
            throw new ConfigurationError(
                    "quarkus.dynamodb.async-client.max-pending-connection-acquires may not be negative or zero.");
        }
        if (asyncClient.eventLoop.override) {
            if (asyncClient.eventLoop.numberOfThreads.isPresent() && asyncClient.eventLoop.numberOfThreads.get() <= 0) {
                throw new ConfigurationError(
                        "quarkus.dynamodb.async-client.event-loop.number-of-threads may not be negative or zero.");
            }
        }
        if (asyncClient.proxy != null && asyncClient.proxy.enabled) {
            URI proxyEndpoint = asyncClient.proxy.endpoint;
            if (proxyEndpoint != null) {
                validateProxyEndpoint(proxyEndpoint, "async");
            }
        }
        validateTlsManagersProvider(asyncClient.tlsManagersProvider, "async");
    }

    private static void validateProxyEndpoint(URI endpoint, String clientType) {
        if (StringUtils.isBlank(endpoint.getScheme())) {
            throw new ConfigurationError(
                    String.format("quarkus.dynamodb.%s-client.proxy.endpoint (%s) - scheme must be specified",
                            clientType, endpoint.toString()));
        }
        if (StringUtils.isBlank(endpoint.getHost())) {
            throw new ConfigurationError(
                    String.format("quarkus.dynamodb.%s-client.proxy.endpoint (%s) - host must be specified",
                            clientType, endpoint.toString()));
        }
        if (StringUtils.isNotBlank(endpoint.getUserInfo())) {
            throw new ConfigurationError(
                    String.format("quarkus.dynamodb.%s-client.proxy.endpoint (%s) - user info is not supported.",
                            clientType, endpoint.toString()));
        }
        if (StringUtils.isNotBlank(endpoint.getPath())) {
            throw new ConfigurationError(
                    String.format("quarkus.dynamodb.%s-client.proxy.endpoint (%s) - path is not supported.",
                            clientType, endpoint.toString()));
        }
        if (StringUtils.isNotBlank(endpoint.getQuery())) {
            throw new ConfigurationError(
                    String.format("quarkus.dynamodb.%s-client.proxy.endpoint (%s) - query is not supported.",
                            clientType, endpoint.toString()));
        }
        if (StringUtils.isNotBlank(endpoint.getFragment())) {
            throw new ConfigurationError(
                    String.format("quarkus.dynamodb.%s-client.proxy.endpoint (%s) - fragment is not supported.",
                            clientType, endpoint.toString()));
        }
    }

    private static void validateTlsManagersProvider(TlsManagersProviderConfig config, String clientType) {
        if (config.type == TlsManagersProviderType.FILE_STORE) {
            if (config.fileStore == null) {
                throw new ConfigurationError(
                        String.format(
                                "quarkus.dynamodb.%s-client.tls-managers-provider.file-store must be specified if 'FILE_STORE' provider type is used",
                                clientType));
            }
            if (config.fileStore.path == null) {
                throw new ConfigurationError(
                        String.format(
                                "quarkus.dynamodb.%s-client.tls-managers-provider.file-store.path should not be empty if 'FILE_STORE' provider is used.",
                                clientType));
            }
            if (StringUtils.isBlank(config.fileStore.type)) {
                throw new ConfigurationError(
                        String.format(
                                "quarkus.dynamodb.%s-client.tls-managers-provider.file-store.type should not be empty if 'FILE_STORE' provider is used.",
                                clientType));
            }
            if (StringUtils.isBlank(config.fileStore.password)) {
                throw new ConfigurationError(
                        String.format(
                                "quarkus.dynamodb.%s-client.tls-managers-provider.file-store.password should not be empty if 'FILE_STORE' provider is used.",
                                clientType));
            }
        }
    }
}
