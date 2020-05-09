package io.quarkus.mongodb.runtime;

import java.util.List;
import java.util.function.Supplier;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.util.AnnotationLiteral;

import com.mongodb.client.MongoClient;
import com.mongodb.event.ConnectionPoolListener;

import io.quarkus.arc.Arc;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MongoClientRecorder {

    public Supplier<MongoClientSupport> mongoClientSupportSupplier(List<String> codecProviders, List<String> bsonDiscriminators,
            List<ConnectionPoolListener> connectionPoolListeners, boolean disableSslSupport) {
        return new Supplier<MongoClientSupport>() {
            @Override
            public MongoClientSupport get() {
                return new MongoClientSupport(codecProviders, bsonDiscriminators, connectionPoolListeners, disableSslSupport);
            }
        };
    }

    public Supplier<MongoClient> mongoClientSupplier(String clientName,
            @SuppressWarnings("usused") MongodbConfig mongodbConfig) {
        MongoClient mongoClient = Arc.container().instance(MongoClients.class).get().createMongoClient(clientName);
        return new Supplier<MongoClient>() {
            @Override
            public MongoClient get() {
                return mongoClient;
            }
        };
    }

    public Supplier<ReactiveMongoClient> reactiveMongoClientSupplier(String clientName,
            @SuppressWarnings("usused") MongodbConfig mongodbConfig) {
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
}
