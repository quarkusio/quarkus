package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.util.AnnotationLiteral;

import com.mongodb.client.MongoClient;
import com.mongodb.event.ConnectionPoolListener;

import io.quarkus.arc.Arc;
import io.quarkus.mongodb.metrics.MicrometerConnectionPoolListener;
import io.quarkus.mongodb.metrics.MongoMetricsConnectionPoolListener;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

@Recorder
public class ElasticsearchLowLevelClientRecorder {

    public Supplier<RestClient> lowlLevelClientSupplier(String clientName) {
        RestClientBuilder builder = RestClientBuilderHelper.createRestClientBuilder(config);

        this.client = builder.build();
        if (config.discovery.enabled) {
            this.sniffer = RestClientBuilderHelper.createSniffer(client, config);
        }
        return new MongoClientSupplier<>(mongoClients -> mongoClients.createMongoClient(clientName));
    }

    public Supplier<ReactiveMongoClient> reactiveMongoClientSupplier(String clientName,
            @SuppressWarnings("unused") MongodbConfig mongodbConfig) {
        return new MongoClientSupplier<>(mongoClients -> mongoClients.createReactiveMongoClient(clientName));
    }

    public RuntimeValue<MongoClient> getClient(String name) {
        return new RuntimeValue<>(Arc.container().instance(MongoClient.class, literal(name)).get());
    }

    public RuntimeValue<ReactiveMongoClient> getReactiveClient(String name) {
        return new RuntimeValue<>(
                Arc.container()
                        .instance(ReactiveMongoClient.class, literal(name + MongoClientBeanUtil.REACTIVE_CLIENT_NAME_SUFFIX))
                        .get());
    }

    @SuppressWarnings("rawtypes")
    private AnnotationLiteral literal(String name) {
        if (name.startsWith(MongoClientBeanUtil.DEFAULT_MONGOCLIENT_NAME)) {
            return Default.Literal.INSTANCE;
        }
        return NamedLiteral.of(name);
    }

    public Supplier<ConnectionPoolListener> createMicrometerConnectionPoolListener() {
        return new Supplier<ConnectionPoolListener>() {
            @Override
            public ConnectionPoolListener get() {
                return MicrometerConnectionPoolListener.createMicrometerConnectionPool();
            }
        };
    }

    public Supplier<ConnectionPoolListener> createMPMetricsConnectionPoolListener() {
        return new Supplier<ConnectionPoolListener>() {
            @Override
            public ConnectionPoolListener get() {
                return new MongoMetricsConnectionPoolListener();
            }
        };
    }
}
