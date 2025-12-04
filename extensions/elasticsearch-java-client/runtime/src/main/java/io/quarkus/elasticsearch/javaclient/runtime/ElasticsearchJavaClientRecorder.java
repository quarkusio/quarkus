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
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ElasticsearchJavaClientRecorder {

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

    public Supplier<ActiveResult> checkActiveHealthCheckSupplier(String clientName) {
        return ActiveResult::active;
    }

    public Supplier<ActiveResult> checkActiveElasticsearchTransportSupplier(String clientName) {
        return ActiveResult::active;
    }
}
