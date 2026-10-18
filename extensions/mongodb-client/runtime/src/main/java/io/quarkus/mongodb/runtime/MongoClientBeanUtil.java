package io.quarkus.mongodb.runtime;

import static io.quarkus.mongodb.runtime.MongoConfig.DEFAULT_CLIENT_NAME;

import java.lang.annotation.Annotation;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.mongodb.MongoClientName;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;

public final class MongoClientBeanUtil {
    private MongoClientBeanUtil() {
        throw new UnsupportedOperationException();
    }

    public static InjectableInstance<MongoClient> mongoClientInstance(String mongoClientName) {
        return Arc.container().select(MongoClient.class, qualifier(mongoClientName));
    }

    public static MongoClient mongoClient() {
        return mongoClientInstance(DEFAULT_CLIENT_NAME).getActive();
    }

    public static MongoClient mongoClient(String mongoClientName) {
        return mongoClientInstance(mongoClientName).getActive();
    }

    public static InjectableInstance<ReactiveMongoClient> mongoClientReactiveInstance(String mongoClientName) {
        return Arc.container().select(ReactiveMongoClient.class, qualifier(mongoClientName));
    }

    public static ReactiveMongoClient reactiveMongoClient() {
        return mongoClientReactiveInstance(DEFAULT_CLIENT_NAME).getActive();
    }

    public static ReactiveMongoClient reactiveMongoClient(String mongoClientName) {
        return mongoClientReactiveInstance(mongoClientName).getActive();
    }

    public static Annotation qualifier(String mongoClientName) {
        if (MongoConfig.isDefaultClient(mongoClientName)) {
            return Default.Literal.INSTANCE;
        } else {
            return MongoClientName.Literal.of(mongoClientName);
        }
    }

    public static String mongoClientName(final Bean<?> bean) {
        for (Object qualifier : bean.getQualifiers()) {
            if (qualifier instanceof MongoClientName mongoClientName) {
                return mongoClientName.value();
            }
        }
        return DEFAULT_CLIENT_NAME;
    }
}
