package io.quarkus.dynamodb.runtime;

import java.net.URI;
import java.util.HashSet;
import java.util.Objects;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkSyncClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient.Builder;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbBaseClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

@ApplicationScoped
public class DynamodbClientProducer {
    private static final Log LOG = LogFactory.getLog(DynamodbClientProducer.class);

    private DynamodbConfig config;
    private DynamoDbClient client;
    private DynamoDbAsyncClient asyncClient;

    public void setConfig(DynamodbConfig config) {
        this.config = config;
    }

    @Produces
    @ApplicationScoped
    public DynamoDbClient client() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder();
        initDynamodbBaseClient(builder, config);
        initHttpClient(builder, config.syncClient);
        client = builder.build();

        return client;
    }

    @Produces
    @ApplicationScoped
    public DynamoDbAsyncClient asyncClient() {
        DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder();
        initDynamodbBaseClient(builder, config);
        initHttpClient(builder, config.asyncClient);
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
        builder.credentialsProvider(config.credentials.type.create(config.credentials));
    }

    private void initSdkClient(SdkClientBuilder builder, SdkConfig config) {
        config.endpointOverride.filter(URI::isAbsolute).ifPresent(builder::endpointOverride);

        if (config.isClientOverrideConfig()) {
            ClientOverrideConfiguration.Builder overrides = ClientOverrideConfiguration.builder();
            config.apiCallTimeout.ifPresent(overrides::apiCallTimeout);
            config.apiCallAttemptTimeout.ifPresent(overrides::apiCallAttemptTimeout);
            config.interceptors.stream()
                    .map(this::createInterceptor)
                    .filter(Objects::nonNull)
                    .forEach(overrides::addExecutionInterceptor);

            builder.overrideConfiguration(overrides.build());
        }
    }

    private void initHttpClient(SdkSyncClientBuilder builder, ApacheHttpClientConfig config) {
        builder.httpClientBuilder(createApacheClientBuilder(config));
    }

    private void initHttpClient(DynamoDbAsyncClientBuilder builder, NettyHttpClientConfig config) {
        builder.httpClientBuilder(createNettyClientBuilder(config));
    }

    private ApacheHttpClient.Builder createApacheClientBuilder(ApacheHttpClientConfig config) {
        Builder builder = ApacheHttpClient.builder();

        config.connectionAcquisitionTimeout.ifPresent(builder::connectionAcquisitionTimeout);
        config.connectionMaxIdleTime.ifPresent(builder::connectionMaxIdleTime);
        config.connectionTimeout.ifPresent(builder::connectionTimeout);
        config.connectionTimeToLive.ifPresent(builder::connectionTimeToLive);
        config.expectContinueEnabled.ifPresent(builder::expectContinueEnabled);
        config.maxConnections.ifPresent(builder::maxConnections);
        config.socketTimeout.ifPresent(builder::socketTimeout);
        config.useIdleConnectionReaper.ifPresent(builder::useIdleConnectionReaper);
        if (config.proxy.enabled) {
            ProxyConfiguration.Builder proxyBuilder = ProxyConfiguration.builder().endpoint(config.proxy.endpoint);
            config.proxy.username.ifPresent(proxyBuilder::username);
            config.proxy.password.ifPresent(proxyBuilder::password);
            config.proxy.nonProxyHosts.forEach(proxyBuilder::addNonProxyHost);
            config.proxy.ntlmDomain.ifPresent(proxyBuilder::ntlmDomain);
            config.proxy.ntlmWorkstation.ifPresent(proxyBuilder::ntlmWorkstation);
            config.proxy.preemptiveBasicAuthenticationEnabled
                    .ifPresent(proxyBuilder::preemptiveBasicAuthenticationEnabled);

            builder.proxyConfiguration(proxyBuilder.build());
        }
        if (config.tlsManagersProvider != null) {
            config.tlsManagersProvider.type.map(type -> type.create(config.tlsManagersProvider))
                    .ifPresent(builder::tlsKeyManagersProvider);
        }

        return builder;
    }

    private NettyNioAsyncHttpClient.Builder createNettyClientBuilder(NettyHttpClientConfig config) {
        NettyNioAsyncHttpClient.Builder builder = NettyNioAsyncHttpClient.builder();

        config.connectionAcquisitionTimeout.ifPresent(builder::connectionAcquisitionTimeout);
        config.connectionMaxIdleTime.ifPresent(builder::connectionMaxIdleTime);
        config.connectionTimeout.ifPresent(builder::connectionTimeout);
        config.connectionTimeToLive.ifPresent(builder::connectionTimeToLive);
        config.maxConcurrency.ifPresent(builder::maxConcurrency);
        config.maxHttp2Streams.ifPresent(builder::maxHttp2Streams);
        config.maxPendingConnectionAcquires.ifPresent(builder::maxPendingConnectionAcquires);
        config.protocol.ifPresent(builder::protocol);
        config.readTimeout.ifPresent(builder::readTimeout);
        config.sslProvider.ifPresent(builder::sslProvider);
        config.useIdleConnectionReaper.ifPresent(builder::useIdleConnectionReaper);
        config.writeTimeout.ifPresent(builder::writeTimeout);

        if (config.proxy.enabled) {
            software.amazon.awssdk.http.nio.netty.ProxyConfiguration.Builder proxyBuilder = software.amazon.awssdk.http.nio.netty.ProxyConfiguration
                    .builder().scheme(config.proxy.endpoint.getScheme())
                    .host(config.proxy.endpoint.getHost())
                    .nonProxyHosts(new HashSet<>(config.proxy.nonProxyHosts));

            if (config.proxy.endpoint.getPort() != -1) {
                proxyBuilder.port(config.proxy.endpoint.getPort());
            }
            builder.proxyConfiguration(proxyBuilder.build());
        }

        if (config.tlsManagersProvider != null) {
            config.tlsManagersProvider.type.map(type -> type.create(config.tlsManagersProvider))
                    .ifPresent(builder::tlsKeyManagersProvider);
        }

        if (config.eventLoop.override) {
            SdkEventLoopGroup.Builder eventLoopBuilder = SdkEventLoopGroup.builder();
            eventLoopBuilder.numberOfThreads(config.eventLoop.numberOfThreads);
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
            return (ExecutionInterceptor) Class.forName(interceptorClass.getName()).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOG.error("Unable to create interceptor", e);
            return null;
        }
    }
}
