package io.quarkus.elasticsearch.javaclient.runtime;

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
import io.smallrye.common.annotation.Identifier;

@Recorder
public class ElasticsearchJavaClientRecorder {

    private final RuntimeValue<ElasticsearchClientsRuntimeConfig> lowLevelRuntimeConfig;

    public ElasticsearchJavaClientRecorder(RuntimeValue<ElasticsearchClientsRuntimeConfig> lowLevelRuntimeConfig) {
        this.lowLevelRuntimeConfig = lowLevelRuntimeConfig;
    }

    public Function<SyntheticCreationalContext<ElasticsearchTransport>, ElasticsearchTransport> elasticsearchTransportSupplier(
            String clientName) {
        return new Function<SyntheticCreationalContext<ElasticsearchTransport>, ElasticsearchTransport>() {
            @Override
            public ElasticsearchTransport apply(SyntheticCreationalContext<ElasticsearchTransport> context) {
                RestClient client = getInjectedReference(context, RestClient.class, clientName);
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
                ElasticsearchTransport transport = getInjectedReference(context, ElasticsearchTransport.class, clientName);
                return new ElasticsearchClient(transport);
            }
        };
    }

    public Function<SyntheticCreationalContext<ElasticsearchAsyncClient>, ElasticsearchAsyncClient> asyncClientSupplier(
            String clientName) {
        return new Function<SyntheticCreationalContext<ElasticsearchAsyncClient>, ElasticsearchAsyncClient>() {
            @Override
            public ElasticsearchAsyncClient apply(SyntheticCreationalContext<ElasticsearchAsyncClient> context) {
                ElasticsearchTransport transport = getInjectedReference(context, ElasticsearchTransport.class, clientName);
                return new ElasticsearchAsyncClient(transport);
            }
        };
    }

    public Supplier<ActiveResult> checkActiveElasticsearchTransportSupplier(String clientName) {
        return () -> {
            ElasticsearchClientRuntimeConfig lowLevelClient = lowLevelRuntimeConfig.getValue().clients().get(clientName);
            if (!lowLevelClient.active()) {
                return ActiveResult.inactive("Elasticsearch Java client [" + clientName + "] is not active, " +
                        "because the corresponding low-level REST client is deactivated through the configuration properties. "
                        + "To activate this client, make sure that "
                        + ElasticsearchClientBeanUtil.activeKey("quarkus.elasticsearch.", clientName)
                        + " is set to true either implicitly (their default values) or explicitly.");
            }
            return ActiveResult.active();
        };
    }

    private static <T, U> T getInjectedReference(SyntheticCreationalContext<U> context, Class<T> type, String clientName) {
        if (ElasticsearchClientBeanUtil.isDefault(clientName)) {
            return context.getInjectedReference(type);
        }
        return context.getInjectedReference(type, Identifier.Literal.of(clientName));
    }
}
