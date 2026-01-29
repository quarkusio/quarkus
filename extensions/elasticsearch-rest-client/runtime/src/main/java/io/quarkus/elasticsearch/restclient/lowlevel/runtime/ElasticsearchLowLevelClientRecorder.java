package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.sniff.ElasticsearchNodesSniffer;
import org.elasticsearch.client.sniff.NodesSniffer;
import org.elasticsearch.client.sniff.Sniffer;
import org.elasticsearch.client.sniff.SnifferBuilder;
import org.jboss.logging.Logger;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.elasticsearch.restclient.common.runtime.ElasticsearchClientBeanUtil;
import io.quarkus.elasticsearch.restclient.lowlevel.ElasticsearchClientConfig;
import io.quarkus.elasticsearch.restclient.lowlevel.runtime.health.ElasticsearchHealthCheckCondition;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;

@Recorder
public class ElasticsearchLowLevelClientRecorder {
    private static final Logger LOG = Logger.getLogger(ElasticsearchLowLevelClientRecorder.class);

    private final RuntimeValue<ElasticsearchClientsRuntimeConfig> runtimeConfig;

    public ElasticsearchLowLevelClientRecorder(RuntimeValue<ElasticsearchClientsRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public Function<SyntheticCreationalContext<RestClient>, RestClient> restClientSupplier(String clientName) {
        return new Function<SyntheticCreationalContext<RestClient>, RestClient>() {
            @Override
            public RestClient apply(SyntheticCreationalContext<RestClient> context) {
                ElasticsearchClientRuntimeConfig config = runtimeConfig.getValue().clients().get(clientName);
                List<HttpHost> hosts = new ArrayList<>(config.hosts().size());
                for (InetSocketAddress host : config.hosts()) {
                    hosts.add(new HttpHost(host.getHostString(), host.getPort(), config.protocol()));
                }

                RestClientBuilder builder = RestClient.builder(hosts.toArray(new HttpHost[0]));

                builder.setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
                    @Override
                    public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder requestConfigBuilder) {
                        return requestConfigBuilder
                                .setConnectTimeout((int) config.connectionTimeout().toMillis())
                                .setSocketTimeout((int) config.socketTimeout().toMillis())
                                .setConnectionRequestTimeout(0); // Avoid requests being flagged as timed out even when they didn't time out.
                    }
                });

                builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        applyAuthentication(httpClientBuilder, config);

                        if (config.ioThreadCounts().isPresent()) {
                            IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                                    .setIoThreadCount(config.ioThreadCounts().get())
                                    .build();
                            httpClientBuilder.setDefaultIOReactorConfig(ioReactorConfig);
                        }

                        httpClientBuilder.setMaxConnTotal(config.maxConnections());
                        httpClientBuilder.setMaxConnPerRoute(config.maxConnectionsPerRoute());

                        if ("http".equalsIgnoreCase(config.protocol())) {
                            // In this case disable the SSL capability as it might have an impact on
                            // bootstrap time, for example consuming entropy for no reason
                            httpClientBuilder.setSSLStrategy(NoopIOSessionStrategy.INSTANCE);
                        }

                        // Apply configuration from RestClientBuilder.HttpClientConfigCallback implementations annotated with ElasticsearchClientConfig
                        HttpAsyncClientBuilder result = httpClientBuilder;
                        Iterable<InstanceHandle<RestClientBuilder.HttpClientConfigCallback>> handles = Arc.container()
                                .select(RestClientBuilder.HttpClientConfigCallback.class,
                                        new ElasticsearchClientConfig.Literal(clientName))
                                .handles();
                        for (InstanceHandle<RestClientBuilder.HttpClientConfigCallback> handle : handles) {
                            result = handle.get().customizeHttpClient(result);
                            handle.close();
                        }
                        return result;
                    }
                });

                return builder.build();
            }
        };
    }

    private static void applyAuthentication(HttpAsyncClientBuilder httpClientBuilder, ElasticsearchClientRuntimeConfig config) {
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
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(config.username().get(), config.password().orElse(null)));
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        } else if (hasApiKey) {
            String apiKey = config.apiKey().get();
            Header apiKeyHeader = new BasicHeader(HttpHeaders.AUTHORIZATION, "ApiKey " + apiKey);
            httpClientBuilder.setDefaultHeaders(Collections.singleton(apiKeyHeader));
        }
    }

    public Function<SyntheticCreationalContext<ElasticsearchHealthCheckCondition>, ElasticsearchHealthCheckCondition> restClientHealthCheckConditionSupplier(
            String clientName) {
        return new Function<SyntheticCreationalContext<ElasticsearchHealthCheckCondition>, ElasticsearchHealthCheckCondition>() {

            @Override
            public ElasticsearchHealthCheckCondition apply(
                    SyntheticCreationalContext<ElasticsearchHealthCheckCondition> context) {
                RestClient restClient = context.getInjectedReference(RestClient.class);
                return new ElasticsearchHealthCheckCondition(clientName, restClient);
            }
        };
    }

    public Function<SyntheticCreationalContext<Sniffer>, Sniffer> restClientSnifferSupplier(String clientName) {
        return new Function<SyntheticCreationalContext<Sniffer>, Sniffer>() {
            @Override
            public Sniffer apply(SyntheticCreationalContext<Sniffer> context) {
                RestClient client = context.getInjectedReference(RestClient.class);
                ElasticsearchClientRuntimeConfig config = runtimeConfig.getValue().clients().get(clientName);

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
        };
    }

    public Supplier<ActiveResult> checkActiveSnifferSupplier(String clientName) {
        return new Supplier<ActiveResult>() {
            @Override
            public ActiveResult get() {
                ActiveResult activeResult = checkActiveRestClientSupplier(clientName).get();
                if (activeResult.value()) {
                    if (runtimeConfig.getValue().clients().get(clientName).discovery().enabled()) {
                        return ActiveResult.active();
                    }
                    return ActiveResult.inactive("Node discovery disabled by the client config.");
                } else {
                    return ActiveResult.inactive("Node discovery disabled as the corresponding rest client is not enabled.",
                            activeResult);
                }
            }
        };
    }

    public Supplier<ActiveResult> checkActiveHealthCheckSupplier(String clientName) {
        return checkActiveRestClientSupplier(clientName);
    }

    public Supplier<ActiveResult> checkActiveRestClientSupplier(String clientName) {
        return () -> {
            ElasticsearchClientRuntimeConfig lowLevelClient = runtimeConfig.getValue().clients().get(clientName);
            if (!lowLevelClient.active()) {
                return ActiveResult.inactive("Elasticsearch low-level REST client [" + clientName + "] is not active, " +
                        "because it is deactivated through the configuration properties. " +
                        "To activate this client, make sure that "
                        + enableKey(clientName) +
                        " is set to true either implicitly (its default values) or explicitly.");
            }
            return ActiveResult.active();
        };
    }

    static String enableKey(String client) {
        return String.format(
                Locale.ROOT, "quarkus.elasticsearch.%sactive",
                ElasticsearchClientBeanUtil.isDefault(client) ? "" : "\"" + client + "\".");
    }
}
