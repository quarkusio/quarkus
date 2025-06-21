package io.quarkus.mongodb.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.util.AnnotationLiteral;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.event.ConnectionPoolListener;

import io.quarkus.arc.Arc;
import io.quarkus.mongodb.metrics.MicrometerConnectionPoolListener;
import io.quarkus.mongodb.metrics.MongoMetricsConnectionPoolListener;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.runtime.dns.MongoDnsClientProvider;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;

@Recorder
public class MongoClientRecorder {
    private final RuntimeValue<MongodbConfig> runtimeConfig;

    public MongoClientRecorder(final RuntimeValue<MongodbConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

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

    /** Helper to lazily create Mongo clients. */
    static final class MongoClientSupplier<T> implements Supplier<T> {
        private final Function<MongoClients, T> producer;

        MongoClientSupplier(Function<MongoClients, T> producer) {
            this.producer = producer;
        }

        @Override
        public T get() {
            // The beans defined in io.quarkus.mongodb.deployment.MongoClientProcessor.createBlockingSyntheticBean and
            // io.quarkus.mongodb.deployment.MongoClientProcessor.createReactiveSyntheticBean are ApplicationScoped,
            // so no need to cache the result here.
            MongoClients mongoClients = Arc.container().instance(MongoClients.class).get();
            return producer.apply(mongoClients);
        }
    }

    public Supplier<MongoClient> mongoClientSupplier(String clientName) {
        return new MongoClientSupplier<>(mongoClients -> mongoClients.createMongoClient(clientName));
    }

    public Supplier<ReactiveMongoClient> reactiveMongoClientSupplier(String clientName) {
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

    /**
     * We need to perform some initialization work on the main thread to ensure that reactive operations (such as DNS
     * resolution)
     * don't end up being performed on the event loop
     */
    public void performInitialization(RuntimeValue<Vertx> vertx) {
        MongoDnsClientProvider.vertx = vertx.getValue();
        initializeDNSLookup(runtimeConfig.getValue().defaultMongoClientConfig());
        for (MongoClientConfig mongoClientConfig : runtimeConfig.getValue().mongoClientConfigs().values()) {
            initializeDNSLookup(mongoClientConfig);
        }
    }

    private void initializeDNSLookup(MongoClientConfig mongoClientConfig) {
        if (mongoClientConfig.connectionString().isEmpty()) {
            return;
        }
        // this ensures that DNS resolution will take place if necessary
        new ConnectionString(mongoClientConfig.connectionString().get());
    }
}
