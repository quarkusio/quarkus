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
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.common.annotation.Identifier;

@Recorder
public class ElasticsearchJavaClientRecorder {

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

    public Supplier<ActiveResult> checkActiveHealthCheckSupplier(String clientName) {
        return ActiveResult::active;
    }

    private static <T, U> T getInjectedReference(SyntheticCreationalContext<U> context, Class<T> type, String clientName) {
        if (ElasticsearchClientBeanUtil.isDefault(clientName)) {
            return context.getInjectedReference(type);
        }
        return context.getInjectedReference(type, Identifier.Literal.of(clientName));
    }

    public Supplier<ActiveResult> checkActiveElasticsearchTransportSupplier(String clientName) {
        return ActiveResult::active;
    }
}
