package io.quarkus.elasticsearch.javaclient.runtime;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import org.elasticsearch.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.elasticsearch.restclient.common.runtime.ElasticsearchClientBeanUtil;
import io.quarkus.elasticsearch.restclient.lowlevel.runtime.ElasticsearchClientRuntimeConfig;
import io.quarkus.elasticsearch.restclient.lowlevel.runtime.ElasticsearchClientsRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ElasticsearchJavaClientRecorder {

    private final RuntimeValue<ElasticsearchJavaClientsRuntimeConfig> runtimeConfig;
    private final RuntimeValue<ElasticsearchClientsRuntimeConfig> lowLevelRuntimeConfig;

    public ElasticsearchJavaClientRecorder(RuntimeValue<ElasticsearchJavaClientsRuntimeConfig> runtimeConfig,
            RuntimeValue<ElasticsearchClientsRuntimeConfig> lowLevelRuntimeConfig) {
        this.runtimeConfig = runtimeConfig;
        this.lowLevelRuntimeConfig = lowLevelRuntimeConfig;
    }

    public Function<SyntheticCreationalContext<ElasticsearchTransport>, ElasticsearchTransport> elasticsearchTransportSupplier(
            String clientName) {
        return new Function<SyntheticCreationalContext<ElasticsearchTransport>, ElasticsearchTransport>() {
            @Override
            public ElasticsearchTransport apply(SyntheticCreationalContext<ElasticsearchTransport> context) {
                RestClient client = context.getInjectedReference(RestClient.class);
                ObjectMapper objectMapper = context.getInjectedReference(ObjectMapper.class);
                return new RestClientTransport(client, new JacksonJsonpMapper(objectMapper));
            }
        };
    }

    public Function<SyntheticCreationalContext<ElasticsearchClient>, ElasticsearchClient> blockingClientSupplier(
            String clientName) {
        return new Function<SyntheticCreationalContext<ElasticsearchClient>, ElasticsearchClient>() {
            @Override
            public ElasticsearchClient apply(SyntheticCreationalContext<ElasticsearchClient> context) {
                ElasticsearchTransport transport = context.getInjectedReference(ElasticsearchTransport.class);
                return new ElasticsearchClient(transport);
            }
        };
    }

    public Function<SyntheticCreationalContext<ElasticsearchAsyncClient>, ElasticsearchAsyncClient> asyncClientSupplier(
            String clientName) {
        return new Function<SyntheticCreationalContext<ElasticsearchAsyncClient>, ElasticsearchAsyncClient>() {
            @Override
            public ElasticsearchAsyncClient apply(SyntheticCreationalContext<ElasticsearchAsyncClient> context) {
                ElasticsearchTransport transport = context.getInjectedReference(ElasticsearchTransport.class);
                return new ElasticsearchAsyncClient(transport);
            }
        };
    }

    public Supplier<ActiveResult> checkActiveElasticsearchTransportSupplier(String clientName) {
        return () -> {
            ElasticsearchClientRuntimeConfig lowLevelClient = lowLevelRuntimeConfig.getValue().clients().get(clientName);
            ElasticsearchJavaClientRuntimeConfig javaClient = runtimeConfig.getValue().clients().get(clientName);
            if (!lowLevelClient.active()) {
                if (javaClient.active().isPresent() && javaClient.active().get()) {
                    throw new IllegalStateException("Elasticsearch Java client [" + clientName + "] is misconfigured. "
                            + "Explicitly enabling this Elasticsearch Java client, "
                            + "while its corresponding low-level Elasticsearch REST client is deactivated ("
                            + enableKey("quarkus.elasticsearch-java.", clientName) + "=false) "
                            + "is not allowed.");
                }
                return ActiveResult.inactive("Elasticsearch Java client [" + clientName + "] is not active, " +
                        "because the corresponding low-level REST client is deactivated through the configuration properties. "
                        + "To activate this client, make sure that both "
                        + enableKey("quarkus.elasticsearch-java.", clientName) + " and "
                        + enableKey("quarkus.elasticsearch.", clientName)
                        + " are set to true either implicitly (their default values) or explicitly.");
            }
            if (!javaClient.active().orElse(true)) {
                return ActiveResult.inactive("Elasticsearch Java client [" + clientName + "] is not active, "
                        + "because it is deactivated through the configuration properties. "
                        + "To activate this client, make sure that "
                        + enableKey("quarkus.elasticsearch-java.", clientName)
                        + " is set to true either implicitly (its default value) or explicitly.");
            }
            return ActiveResult.active();
        };
    }

    static String enableKey(String radical, String client) {
        return String.format(
                Locale.ROOT, "%s%sactive",
                radical,
                ElasticsearchClientBeanUtil.isDefault(client) ? "" : "\"" + client + "\".");
    }
}
