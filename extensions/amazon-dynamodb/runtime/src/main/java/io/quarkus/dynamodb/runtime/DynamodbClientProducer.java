package io.quarkus.dynamodb.runtime;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.quarkus.dynamodb.runtime.SyncHttpClientBuildTimeConfig.SyncClientType;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient.Builder;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbBaseClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

@ApplicationScoped
public class DynamodbClientProducer {
    private static final Log LOG = LogFactory.getLog(DynamodbClientProducer.class);

    @Inject
    DynamodbConfig runtimeConfig;
    private DynamoDbClient client;
    private DynamoDbAsyncClient asyncClient;
    @Inject
    DynamodbBuildTimeConfig buildTimeConfig;

    @Produces
    @ApplicationScoped
    public DynamoDbClient client() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder();
        initDynamodbBaseClient(builder, runtimeConfig);
        initHttpClient(builder, runtimeConfig.syncClient);
        client = builder.build();

        return client;
    }

    @Produces
    @ApplicationScoped
    public DynamoDbAsyncClient asyncClient() {
        DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder();
        initDynamodbBaseClient(builder, runtimeConfig);
        initHttpClient(builder, runtimeConfig.asyncClient);
        asyncClient = builder.build();

        return asyncClient;
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            client.close();
        }
        if (asyncClient != null) {
            asyncClient.close();
        }
    }

    private void initDynamodbBaseClient(DynamoDbBaseClientBuilder builder, DynamodbConfig config) {
        if (config.enableEndpointDiscovery) {
            builder.enableEndpointDiscovery();
        }
        initAwsClient(builder, config.aws);
        initSdkClient(builder, config.sdk);
    }

    private void initAwsClient(AwsClientBuilder builder, AwsConfig config) {
        config.region.ifPresent(builder::region);

        if (config.credentials.type == AwsCredentialsProviderType.STATIC) {
            if (!config.credentials.staticProvider.accessKeyId.isPresent()
                    || !config.credentials.staticProvider.secretAccessKey.isPresent()) {
                throw new RuntimeConfigurationError(
                        "quarkus.dynamodb.aws.credentials.static-provider.access-key-id and "
                                + "quarkus.dynamodb.aws.credentials.static-provider.secret-access-key cannot be empty if STATIC credentials provider used.");
            }
        }
        if (config.credentials.type == AwsCredentialsProviderType.PROCESS) {
            if (!config.credentials.processProvider.command.isPresent()) {
                throw new RuntimeConfigurationError(
                        "quarkus.dynamodb.aws.credentials.process-provider.command cannot be empty if PROCESS credentials provider used.");
            }
        }
        builder.credentialsProvider(config.credentials.type.create(config.credentials));
    }

    private void initSdkClient(SdkClientBuilder builder, SdkConfig config) {
        if (config.endpointOverride.isPresent()) {
            URI endpointOverride = config.endpointOverride.get();
            if (StringUtils.isBlank(endpointOverride.getScheme())) {
                throw new RuntimeConfigurationError(
                        String.format("quarkus.dynamodb.sdk.endpoint-override (%s) - scheme must be specified",
                                endpointOverride.toString()));
            }
            builder.endpointOverride(endpointOverride);
        }

        ClientOverrideConfiguration.Builder overrides = ClientOverrideConfiguration.builder();
        config.apiCallTimeout.ifPresent(overrides::apiCallTimeout);
        config.apiCallAttemptTimeout.ifPresent(overrides::apiCallAttemptTimeout);
        buildTimeConfig.sdk.interceptors.orElse(Collections.emptyList()).stream()
                .map(this::createInterceptor)
                .filter(Objects::nonNull)
                .forEach(overrides::addExecutionInterceptor);

        builder.overrideConfiguration(overrides.build());
    }

    private void initHttpClient(DynamoDbClientBuilder builder, SyncHttpClientConfig config) {
        if (buildTimeConfig.syncClient.type == SyncClientType.APACHE) {
            validateApacheClientConfig(config);
            builder.httpClientBuilder(createApacheClientBuilder(config));
        } else {
            builder.httpClientBuilder(createUrlConnectionClientBuilder(config));
        }
    }

    private void initHttpClient(DynamoDbAsyncClientBuilder builder, NettyHttpClientConfig config) {
        validateNettyClientConfig(config);
        builder.httpClientBuilder(createNettyClientBuilder(config));
    }

    private UrlConnectionHttpClient.Builder createUrlConnectionClientBuilder(SyncHttpClientConfig config) {
        UrlConnectionHttpClient.Builder builder = UrlConnectionHttpClient.builder();
        builder.connectionTimeout(config.connectionTimeout);
        builder.socketTimeout(config.socketTimeout);
        return builder;
    }

    private ApacheHttpClient.Builder createApacheClientBuilder(SyncHttpClientConfig config) {
        Builder builder = ApacheHttpClient.builder();

        builder.connectionTimeout(config.connectionTimeout);
        builder.socketTimeout(config.socketTimeout);

        builder.connectionAcquisitionTimeout(config.apache.connectionAcquisitionTimeout);
        builder.connectionMaxIdleTime(config.apache.connectionMaxIdleTime);
        config.apache.connectionTimeToLive.ifPresent(builder::connectionTimeToLive);
        builder.expectContinueEnabled(config.apache.expectContinueEnabled);
        builder.maxConnections(config.apache.maxConnections);
        builder.useIdleConnectionReaper(config.apache.useIdleConnectionReaper);

        if (config.apache.proxy.enabled && config.apache.proxy.endpoint.isPresent()) {
            ProxyConfiguration.Builder proxyBuilder = ProxyConfiguration.builder()
                    .endpoint(config.apache.proxy.endpoint.get());
            config.apache.proxy.username.ifPresent(proxyBuilder::username);
            config.apache.proxy.password.ifPresent(proxyBuilder::password);
            config.apache.proxy.nonProxyHosts.ifPresent(c -> c.forEach(proxyBuilder::addNonProxyHost));
            config.apache.proxy.ntlmDomain.ifPresent(proxyBuilder::ntlmDomain);
            config.apache.proxy.ntlmWorkstation.ifPresent(proxyBuilder::ntlmWorkstation);
            config.apache.proxy.preemptiveBasicAuthenticationEnabled
                    .ifPresent(proxyBuilder::preemptiveBasicAuthenticationEnabled);

            builder.proxyConfiguration(proxyBuilder.build());
        }

        builder.tlsKeyManagersProvider(
                config.apache.tlsManagersProvider.type.create(config.apache.tlsManagersProvider));

        return builder;
    }

    private NettyNioAsyncHttpClient.Builder createNettyClientBuilder(NettyHttpClientConfig config) {
        NettyNioAsyncHttpClient.Builder builder = NettyNioAsyncHttpClient.builder();

        builder.connectionAcquisitionTimeout(config.connectionAcquisitionTimeout);
        builder.connectionMaxIdleTime(config.connectionMaxIdleTime);
        builder.connectionTimeout(config.connectionTimeout);
        config.connectionTimeToLive.ifPresent(builder::connectionTimeToLive);
        builder.maxConcurrency(config.maxConcurrency);
        builder.maxHttp2Streams(config.maxHttp2Streams);
        builder.maxPendingConnectionAcquires(config.maxPendingConnectionAcquires);
        builder.protocol(config.protocol);
        builder.readTimeout(config.readTimeout);
        builder.writeTimeout(config.writeTimeout);
        config.sslProvider.ifPresent(builder::sslProvider);
        builder.useIdleConnectionReaper(config.useIdleConnectionReaper);

        if (config.proxy.enabled && config.proxy.endpoint.isPresent()) {
            software.amazon.awssdk.http.nio.netty.ProxyConfiguration.Builder proxyBuilder = software.amazon.awssdk.http.nio.netty.ProxyConfiguration
                    .builder().scheme(config.proxy.endpoint.get().getScheme())
                    .host(config.proxy.endpoint.get().getHost())
                    .nonProxyHosts(new HashSet<>(config.proxy.nonProxyHosts.orElse(Collections.emptyList())));

            if (config.proxy.endpoint.get().getPort() != -1) {
                proxyBuilder.port(config.proxy.endpoint.get().getPort());
            }
            builder.proxyConfiguration(proxyBuilder.build());
        }

        builder.tlsKeyManagersProvider(config.tlsManagersProvider.type.create(config.tlsManagersProvider));

        if (config.eventLoop.override) {
            SdkEventLoopGroup.Builder eventLoopBuilder = SdkEventLoopGroup.builder();
            config.eventLoop.numberOfThreads.ifPresent(eventLoopBuilder::numberOfThreads);
            if (config.eventLoop.threadNamePrefix.isPresent()) {
                eventLoopBuilder.threadFactory(
                        new ThreadFactoryBuilder().threadNamePrefix(config.eventLoop.threadNamePrefix.get()).build());
            }
            builder.eventLoopGroupBuilder(eventLoopBuilder);
        }

        return builder;
    }

    private ExecutionInterceptor createInterceptor(Class<?> interceptorClass) {
        try {
            return (ExecutionInterceptor) Class
                    .forName(interceptorClass.getName(), true, Thread.currentThread().getContextClassLoader()).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOG.error("Unable to create interceptor", e);
            return null;
        }
    }

    private void validateApacheClientConfig(SyncHttpClientConfig config) {
        if (config.apache.maxConnections <= 0) {
            throw new RuntimeConfigurationError("quarkus.dynamodb.sync-client.max-connections may not be negative or zero.");
        }
        if (config.apache.proxy.enabled) {
            config.apache.proxy.endpoint.ifPresent(u -> validateProxyEndpoint(u, "sync"));
        }
        validateTlsManagersProvider(config.apache.tlsManagersProvider, "sync");
    }

    private void validateNettyClientConfig(NettyHttpClientConfig asyncClient) {
        if (asyncClient.maxConcurrency <= 0) {
            throw new RuntimeConfigurationError("quarkus.dynamodb.async-client.max-concurrency may not be negative or zero.");
        }
        if (asyncClient.maxHttp2Streams < 0) {
            throw new RuntimeConfigurationError(
                    "quarkus.dynamodb.async-client.max-http2-streams may not be negative.");
        }
        if (asyncClient.maxPendingConnectionAcquires <= 0) {
            throw new RuntimeConfigurationError(
                    "quarkus.dynamodb.async-client.max-pending-connection-acquires may not be negative or zero.");
        }
        if (asyncClient.eventLoop.override) {
            if (asyncClient.eventLoop.numberOfThreads.isPresent() && asyncClient.eventLoop.numberOfThreads.get() <= 0) {
                throw new RuntimeConfigurationError(
                        "quarkus.dynamodb.async-client.event-loop.number-of-threads may not be negative or zero.");
            }
        }
        if (asyncClient.proxy.enabled) {
            asyncClient.proxy.endpoint.ifPresent(proxyEndpoint -> validateProxyEndpoint(proxyEndpoint, "async"));
        }
        validateTlsManagersProvider(asyncClient.tlsManagersProvider, "async");
    }

    private void validateProxyEndpoint(URI endpoint, String clientType) {
        if (StringUtils.isBlank(endpoint.getScheme())) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.dynamodb.%s-client.proxy.endpoint (%s) - scheme must be specified",
                            clientType, endpoint.toString()));
        }
        if (StringUtils.isBlank(endpoint.getHost())) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.dynamodb.%s-client.proxy.endpoint (%s) - host must be specified",
                            clientType, endpoint.toString()));
        }
        if (StringUtils.isNotBlank(endpoint.getUserInfo())) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.dynamodb.%s-client.proxy.endpoint (%s) - user info is not supported.",
                            clientType, endpoint.toString()));
        }
        if (StringUtils.isNotBlank(endpoint.getPath())) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.dynamodb.%s-client.proxy.endpoint (%s) - path is not supported.",
                            clientType, endpoint.toString()));
        }
        if (StringUtils.isNotBlank(endpoint.getQuery())) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.dynamodb.%s-client.proxy.endpoint (%s) - query is not supported.",
                            clientType, endpoint.toString()));
        }
        if (StringUtils.isNotBlank(endpoint.getFragment())) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.dynamodb.%s-client.proxy.endpoint (%s) - fragment is not supported.",
                            clientType, endpoint.toString()));
        }
    }

    private void validateTlsManagersProvider(TlsManagersProviderConfig config, String clientType) {
        if (config.type == TlsManagersProviderType.FILE_STORE) {
            if (!config.fileStore.isPresent()) {
                throw new RuntimeConfigurationError(
                        String.format(
                                "quarkus.dynamodb.%s-client.tls-managers-provider.file-store must be specified if 'FILE_STORE' provider type is used",
                                clientType));
            }
        }
    }
}
