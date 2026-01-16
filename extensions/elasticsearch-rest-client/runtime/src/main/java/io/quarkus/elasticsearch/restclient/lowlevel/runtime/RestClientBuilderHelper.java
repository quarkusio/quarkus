package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.net.NamedEndpoint;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.reactor.ssl.TransportSecurityLayer;
import org.apache.hc.core5.util.Timeout;
import org.jboss.logging.Logger;

import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.ElasticsearchNodesSniffer;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.NodesSniffer;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.Sniffer;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.SnifferBuilder;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.elasticsearch.restclient.lowlevel.ElasticsearchClientConfig;
import io.quarkus.runtime.configuration.ConfigurationException;

public final class RestClientBuilderHelper {

    private static final Logger LOG = Logger.getLogger(RestClientBuilderHelper.class);

    private RestClientBuilderHelper() {
        // avoid instantiation
    }

    public static Rest5ClientBuilder createRestClientBuilder(ElasticsearchConfig config) {
        List<HttpHost> hosts = new ArrayList<>(config.hosts().size());
        for (InetSocketAddress host : config.hosts()) {
            hosts.add(new HttpHost(config.protocol(), host.getHostString(), host.getPort()));
        }

        Rest5ClientBuilder builder = Rest5Client.builder(hosts.toArray(new HttpHost[0]));

        builder.setRequestConfigCallback(new Consumer<>() {
            @Override
            public void accept(RequestConfig.Builder requestConfigBuilder) {
                requestConfigBuilder
                        .setConnectionRequestTimeout(Timeout.INFINITE); // Avoid requests being flagged as timed out even when they didn't time out.
            }
        });

        builder.setHttpClientConfigCallback(new Consumer<>() {
            @Override
            public void accept(HttpAsyncClientBuilder httpClientBuilder) {
                applyAuthentication(httpClientBuilder, config);

                if (config.ioThreadCounts().isPresent()) {
                    IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                            .setIoThreadCount(config.ioThreadCounts().get())
                            .build();
                    httpClientBuilder.setIOReactorConfig(ioReactorConfig);
                }

                PoolingAsyncClientConnectionManagerBuilder connectionManagerBuilder = PoolingAsyncClientConnectionManagerBuilder
                        .create()
                        .setMaxConnTotal(config.maxConnections())
                        .setMaxConnPerRoute(config.maxConnectionsPerRoute());

                if ("http".equalsIgnoreCase(config.protocol())) {
                    // In this case disable the SSL capability as it might have an impact on
                    // bootstrap time, for example consuming entropy for no reason
                    // connectionManagerBuilder.setTlsStrategy(
                    //         ClientTlsStrategyBuilder.create()
                    //                 .setSslContext(
                    //                         SSLContextBuilder.create()
                    //                                 .loadTrustMaterial(null, new TrustAllStrategy())
                    //                                 .buildAsync()
                    //                 )
                    //                 .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    //                 .build()
                    // );
                    connectionManagerBuilder.setTlsStrategy(NoopTlsStrategy.INSTANCE);
                }

                httpClientBuilder.setConnectionManager(
                        connectionManagerBuilder
                                .setDefaultConnectionConfig(
                                        ConnectionConfig.copy(ConnectionConfig.DEFAULT)
                                                .setConnectTimeout(Timeout.of(config.connectionTimeout()))
                                                .setSocketTimeout(Timeout.of(config.socketTimeout()))
                                                .build())
                                .build());

                // Apply configuration from ElasticsearchClientConfig.Configurer implementations annotated with ElasticsearchClientConfig
                Iterable<InstanceHandle<ElasticsearchClientConfig.Configurer>> handles = Arc.container()
                        .select(ElasticsearchClientConfig.Configurer.class, new ElasticsearchClientConfig.Literal())
                        .handles();
                for (InstanceHandle<ElasticsearchClientConfig.Configurer> handle : handles) {
                    handle.get().accept(httpClientBuilder);
                    handle.close();
                }
            }
        });

        return builder;
    }

    public static Sniffer createSniffer(Rest5Client client, ElasticsearchConfig config) {
        SnifferBuilder builder = Sniffer.builder(client)
                .setSniffIntervalMillis((int) config.discovery().refreshInterval().toMillis());

        // https discovery support
        if ("https".equalsIgnoreCase(config.protocol())) {
            NodesSniffer hostsSniffer = new ElasticsearchNodesSniffer(
                    client,
                    ElasticsearchNodesSniffer.DEFAULT_SNIFF_REQUEST_TIMEOUT, // 1sec
                    ElasticsearchNodesSniffer.Scheme.HTTPS);
            builder.setNodesSniffer(hostsSniffer);
        }

        return builder.build();
    }

    private static void applyAuthentication(HttpAsyncClientBuilder httpClientBuilder, ElasticsearchConfig config) {
        boolean hasBasic = config.username().isPresent();
        boolean hasApiKey = config.apiKey().isPresent();
        if (hasBasic && hasApiKey) {
            throw new ConfigurationException("You must provide either a valid username/password pair for Basic " +
                    "authentication OR only a valid API key for ApiKey authentication. Both methods are currently " +
                    "enabled.");
        }
        if (!"https".equalsIgnoreCase(config.protocol()) && (hasBasic || hasApiKey)) {
            LOG.warn("Transmitting authentication information over HTTP is unsafe as it implies sending sensitive " +
                    "information as plain text over an unencrypted channel. Use the HTTPS protocol instead.");
        }
        if (hasBasic) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(null, null, -1, null, null),
                    new UsernamePasswordCredentials(config.username().get(), config.password().orElse(null)));
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        } else if (hasApiKey) {
            String apiKey = config.apiKey().get();
            Header apiKeyHeader = new BasicHeader(HttpHeaders.AUTHORIZATION, "ApiKey " + apiKey);
            httpClientBuilder.setDefaultHeaders(Collections.singleton(apiKeyHeader));
        }
    }

    private static class NoopTlsStrategy implements TlsStrategy {
        private static final NoopTlsStrategy INSTANCE = new NoopTlsStrategy();

        private NoopTlsStrategy() {
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean upgrade(TransportSecurityLayer sessionLayer, HttpHost host, SocketAddress localAddress,
                SocketAddress remoteAddress, Object attachment, Timeout handshakeTimeout) {
            throw new UnsupportedOperationException("upgrade is not supported.");
        }

        @Override
        public void upgrade(TransportSecurityLayer sessionLayer, NamedEndpoint endpoint, Object attachment,
                Timeout handshakeTimeout, FutureCallback<TransportSecurityLayer> callback) {
            if (callback != null) {
                callback.completed(sessionLayer);
            }
        }
    }
}
