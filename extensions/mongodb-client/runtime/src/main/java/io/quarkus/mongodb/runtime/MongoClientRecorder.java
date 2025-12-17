package io.quarkus.mongodb.runtime;

import static io.quarkus.mongodb.runtime.MongoConfig.getPropertyName;

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

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.Arc;
import io.quarkus.mongodb.metrics.MicrometerConnectionPoolListener;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.runtime.dns.MongoDnsClientProvider;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;

@Recorder
public class MongoClientRecorder {
    private final RuntimeValue<MongoConfig> runtimeConfig;

    public MongoClientRecorder(final RuntimeValue<MongoConfig> runtimeConfig) {
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
        if (name.startsWith(MongoConfig.DEFAULT_CLIENT_NAME)) {
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

    /**
     * We need to perform some initialization work on the main thread to ensure that reactive operations (such as DNS
     * resolution)
     * don't end up being performed on the event loop
     */
    public void performInitialization(RuntimeValue<Vertx> vertx) {
        MongoDnsClientProvider.vertx = vertx.getValue();
        for (MongoClientConfig mongoClientConfig : runtimeConfig.getValue().clients().values()) {
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

    public Supplier<ActiveResult> checkActive(final String name) {
        return new Supplier<>() {
            @Override
            public ActiveResult get() {
                MongoClientConfig mongoClientConfig = runtimeConfig.getValue().clients().get(name);
                if (!mongoClientConfig.active()) {
                    return ActiveResult.inactive(String.format(
                            """
                                    Mongo Client '%s' was deactivated through configuration properties. \
                                    To activate the Mongo Client, set configuration property '%s' to 'true' and configure the Mongo Client '%s'. \
                                    Refer to https://quarkus.io/guides/mongodb for guidance.
                                    """,
                            name, getPropertyName(name, "active"), name));
                }
                if (mongoClientConfig.hosts().isEmpty() && mongoClientConfig.connectionString().isEmpty()) {
                    return ActiveResult.inactive(String.format(
                            """
                                    Mongo Client '%s' was deactivated automatically because neither the hosts nor the connectionString is set. \
                                    To activate the Mongo Client, set the configuration property '%s' or '%s' \
                                    Refer to https://quarkus.io/guides/mongodb for guidance.
                                    """,
                            name, getPropertyName(name, "hosts"), getPropertyName(name, "connection-string")));
                }
                return ActiveResult.active();
            }
        };
    }
}
