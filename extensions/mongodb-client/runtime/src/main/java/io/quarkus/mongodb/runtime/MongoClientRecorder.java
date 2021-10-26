package io.quarkus.mongodb.runtime;

import java.util.ArrayList;
import java.util.List;
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

@Recorder
public class MongoClientRecorder {

    public Supplier<MongoClientSupport> mongoClientSupportSupplier(List<String> bsonDiscriminators,
            List<Supplier<ConnectionPoolListener>> connectionPoolListenerSuppliers, boolean disableSslSupport) {

        return new Supplier<MongoClientSupport>() {
            @Override
            public MongoClientSupport get() {

                List<ConnectionPoolListener> connectionPoolListeners = new ArrayList<>(connectionPoolListenerSuppliers.size());
                for (Supplier<ConnectionPoolListener> item : connectionPoolListenerSuppliers) {
                    connectionPoolListeners.add(item.get());
                }

                return new MongoClientSupport(bsonDiscriminators,
                        connectionPoolListeners, disableSslSupport);
            }
        };
    }

    public Supplier<MongoClient> mongoClientSupplier(String clientName,
            @SuppressWarnings("unused") MongodbConfig mongodbConfig) {
        MongoClient mongoClient = Arc.container().instance(MongoClients.class).get().createMongoClient(clientName);
        return new Supplier<MongoClient>() {
            @Override
            public MongoClient get() {
                return mongoClient;
            }
        };
    }

    public Supplier<ReactiveMongoClient> reactiveMongoClientSupplier(String clientName,
            @SuppressWarnings("unused") MongodbConfig mongodbConfig) {
        ReactiveMongoClient reactiveMongoClient = Arc.container().instance(MongoClients.class).get()
                .createReactiveMongoClient(clientName);
        return new Supplier<ReactiveMongoClient>() {
            @Override
            public ReactiveMongoClient get() {
                return reactiveMongoClient;
            }
        };
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
