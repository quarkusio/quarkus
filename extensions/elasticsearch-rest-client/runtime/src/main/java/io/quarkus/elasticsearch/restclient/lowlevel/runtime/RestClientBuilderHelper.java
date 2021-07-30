package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.sniff.ElasticsearchNodesSniffer;
import org.elasticsearch.client.sniff.NodesSniffer;
import org.elasticsearch.client.sniff.Sniffer;
import org.elasticsearch.client.sniff.SnifferBuilder;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.elasticsearch.restclient.lowlevel.ElasticsearchClientConfig;

public final class RestClientBuilderHelper {

    private static final Logger LOG = Logger.getLogger(RestClientBuilderHelper.class);

    private RestClientBuilderHelper() {
        // avoid instantiation
    }

    public static RestClientBuilder createRestClientBuilder(ElasticsearchConfig config) {
        List<HttpHost> hosts = new ArrayList<>(config.hosts.size());
        for (InetSocketAddress host : config.hosts) {
            hosts.add(new HttpHost(host.getHostString(), host.getPort(), config.protocol));
        }

        RestClientBuilder builder = RestClient.builder(hosts.toArray(new HttpHost[0]));

        builder.setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
            @Override
            public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder requestConfigBuilder) {
                return requestConfigBuilder
                        .setConnectTimeout((int) config.connectionTimeout.toMillis())
                        .setSocketTimeout((int) config.socketTimeout.toMillis())
                        .setConnectionRequestTimeout(0); // Avoid requests being flagged as timed out even when they didn't time out.
            }
        });

        builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                if (config.username.isPresent()) {
                    if (!"https".equalsIgnoreCase(config.protocol)) {
                        LOG.warn("Using Basic authentication in HTTP implies sending plain text passwords over the wire, " +
                                "use the HTTPS protocol instead.");
                    }
                    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(config.username.get(), config.password.orElse(null)));
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }

                if (config.ioThreadCounts.isPresent()) {
                    IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                            .setIoThreadCount(config.ioThreadCounts.get())
                            .build();
                    httpClientBuilder.setDefaultIOReactorConfig(ioReactorConfig);
                }

                httpClientBuilder.setMaxConnTotal(config.maxConnections);
                httpClientBuilder.setMaxConnPerRoute(config.maxConnectionsPerRoute);

                if ("http".equalsIgnoreCase(config.protocol)) {
                    // In this case disable the SSL capability as it might have an impact on
                    // bootstrap time, for example consuming entropy for no reason
                    httpClientBuilder.setSSLStrategy(NoopIOSessionStrategy.INSTANCE);
                }

                // Apply configuration from RestClientBuilder.HttpClientConfigCallback implementations annotated with ElasticsearchClientConfig
                HttpAsyncClientBuilder result = httpClientBuilder;
                Iterable<InstanceHandle<RestClientBuilder.HttpClientConfigCallback>> handles = Arc.container()
                        .select(RestClientBuilder.HttpClientConfigCallback.class, new ElasticsearchClientConfig.Literal())
                        .handles();
                for (InstanceHandle<RestClientBuilder.HttpClientConfigCallback> handle : handles) {
                    result = handle.get().customizeHttpClient(result);
                    handle.close();
                }
                return result;
            }
        });

        return builder;
    }

    public static Sniffer createSniffer(RestClient client, ElasticsearchConfig config) {
        SnifferBuilder builder = Sniffer.builder(client)
                .setSniffIntervalMillis((int) config.discovery.refreshInterval.toMillis());

        // https discovery support
        if ("https".equalsIgnoreCase(config.protocol)) {
            NodesSniffer hostsSniffer = new ElasticsearchNodesSniffer(
                    client,
                    ElasticsearchNodesSniffer.DEFAULT_SNIFF_REQUEST_TIMEOUT, // 1sec
                    ElasticsearchNodesSniffer.Scheme.HTTPS);
            builder.setNodesSniffer(hostsSniffer);
        }

        return builder.build();
    }
}
