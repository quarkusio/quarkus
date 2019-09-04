package io.quarkus.dynamodb.runtime;

import java.net.URI;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import software.amazon.awssdk.core.client.builder.SdkAsyncClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkSyncClientBuilder;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient.Builder;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.services.dynamodb.*;

@ApplicationScoped
public class DynamodbClientProducer {
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
        initHttpClient(builder, config);
        client = builder.build();

        return client;
    }

    @Produces
    @ApplicationScoped
    public DynamoDbAsyncClient asyncClient() {
        DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder();
        initDynamodbBaseClient(builder, config);
        initHttpClient(builder, config);
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
        config.region.ifPresent(builder::region);

        builder.credentialsProvider(config.credentials.type.create(config.credentials));

        config.endpointOverride.filter(URI::isAbsolute).ifPresent(builder::endpointOverride);

        if (config.enableEndpointDiscovery) {
            builder.enableEndpointDiscovery();
        }
    }

    private void initHttpClient(SdkSyncClientBuilder builder, DynamodbConfig config) {
        builder.httpClientBuilder(createApacheClientBuilder(config.syncClient));
    }

    private void initHttpClient(SdkAsyncClientBuilder builder, DynamodbConfig config) {
        // Until we have a compatible version of the AWS SDK with the Netty version used in Quarkus, disable the
        // async support.
        //        builder.httpClientBuilder(createNettyClientBuilder(config.asyncClient));
    }

    private ApacheHttpClient.Builder createApacheClientBuilder(AwsApacheHttpClientConfig config) {
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

        return builder;
    }

    // Until we have a compatible version of the AWS SDK with the Netty version used in Quarkus, disable the
    // async support.
    //    private NettyNioAsyncHttpClient.Builder createNettyClientBuilder(AwsNettyNioAsyncHttpClientConfig config) {
    //        NettyNioAsyncHttpClient.Builder builder = NettyNioAsyncHttpClient.builder();
    //
    //        config.connectionAcquisitionTimeout.ifPresent(builder::connectionAcquisitionTimeout);
    //        config.connectionMaxIdleTime.ifPresent(builder::connectionMaxIdleTime);
    //        config.connectionTimeout.ifPresent(builder::connectionTimeout);
    //        config.connectionTimeToLive.ifPresent(builder::connectionTimeToLive);
    //        config.maxConcurrency.ifPresent(builder::maxConcurrency);
    //        config.maxHttp2Streams.ifPresent(builder::maxHttp2Streams);
    //        config.maxPendingConnectionAcquires.ifPresent(builder::maxPendingConnectionAcquires);
    //        config.protocol.ifPresent(builder::protocol);
    //        config.readTimeout.ifPresent(builder::readTimeout);
    //        config.sslProvider.ifPresent(builder::sslProvider);
    //        config.useIdleConnectionReaper.ifPresent(builder::useIdleConnectionReaper);
    //        config.writeTimeout.ifPresent(builder::writeTimeout);
    //
    //        if (config.eventLoop.override) {
    //            SdkEventLoopGroup.Builder eventLoopBuilder = SdkEventLoopGroup.builder();
    //            eventLoopBuilder.numberOfThreads(config.eventLoop.numberOfThreads);
    //            if (config.eventLoop.threadNamePrefix.isPresent()) {
    //                eventLoopBuilder.threadFactory(
    //                        new ThreadFactoryBuilder().threadNamePrefix(config.eventLoop.threadNamePrefix.get()).build());
    //            }
    //            builder.eventLoopGroupBuilder(eventLoopBuilder);
    //        }
    //
    //        return builder;
    //    }
}
