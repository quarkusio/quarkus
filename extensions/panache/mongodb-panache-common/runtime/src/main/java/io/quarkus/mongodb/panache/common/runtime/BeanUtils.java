package io.quarkus.mongodb.panache.common.runtime;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Predicate;

import jakarta.inject.Named;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.mongodb.MongoClientName;
import io.quarkus.mongodb.panache.common.MongoDatabaseResolver;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.runtime.MongoClientBeanUtil;
import io.quarkus.mongodb.runtime.MongoClientConfig;
import io.quarkus.mongodb.runtime.MongoConfig;
import io.smallrye.config.SmallRyeConfig;

public final class BeanUtils {

    private BeanUtils() {
    }

    public static String beanName(MongoEntity entity) {
        if (entity != null && !entity.clientName().isEmpty()) {
            return entity.clientName();
        }

        return MongoConfig.DEFAULT_CLIENT_NAME;
    }

    public static <T> T clientFromArc(MongoEntity entity,
            Class<T> clientClass, boolean isReactive) {
        // we must consider multiple instances if @MongoClientName is used in client code
        T mongoClient = firstInstanceWithoutQualifier(
                Arc.container().select(clientClass, MongoClientBeanUtil.clientLiteral(beanName(entity), isReactive)).handles(),
                MongoClientName.class);
        if (mongoClient != null) {
            return mongoClient;
        }

        if ((entity == null || entity.clientName().isEmpty())) {
            // this case happens when there are multiple instances because they are all annotated with @Named
            mongoClient = firstInstanceWithoutQualifier(Arc.container().select(clientClass).handles(), Named.class);
            if (mongoClient != null) {
                return mongoClient;
            }
            throw new IllegalStateException(String.format("Unable to find default %s bean", clientClass.getSimpleName()));
        } else {
            throw new IllegalStateException(
                    String.format("Unable to find %s bean for entity %s", clientClass.getSimpleName(), entity));
        }
    }

    public static String getDatabaseName(MongoEntity mongoEntity, String clientBeanName) {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        MongoConfig mongoConfig = config.getConfigMapping(MongoConfig.class);
        MongoClientConfig mongoClientConfig = mongoConfig.clients().get(clientBeanName);
        if (mongoClientConfig.database().isPresent()) {
            return mongoClientConfig.database().get();
        }
        mongoClientConfig = mongoConfig.clients().get(MongoConfig.DEFAULT_CLIENT_NAME);
        if (mongoClientConfig.database().isPresent()) {
            return mongoClientConfig.database().get();
        }

        if (mongoEntity == null) {
            throw new IllegalArgumentException(
                    "The database property was not configured for the default Mongo Client (via 'quarkus.mongodb.database'");
        }
        if (mongoEntity.clientName().isEmpty()) {
            throw new IllegalArgumentException("The database attribute was not set for the @MongoEntity annotation "
                    + "and neither was the database property configured for the default Mongo Client (via 'quarkus.mongodb.database')");
        }
        throw new IllegalArgumentException(String.format(
                "The database attribute was not set for the @MongoEntity annotation neither was the database property configured for the named Mongo Client (via 'quarkus.mongodb.%s.database')",
                mongoEntity.clientName()));
    }

    public static Optional<String> getDatabaseNameFromResolver() {
        return Optional.of(Arc.container().select(MongoDatabaseResolver.class))
                .filter(Predicate.not(InjectableInstance::isUnsatisfied))
                .map(InjectableInstance::get)
                .map(MongoDatabaseResolver::resolve)
                .filter(Predicate.not(String::isBlank));
    }

    private static <T> T firstInstanceWithoutQualifier(Iterable<InstanceHandle<T>> handles,
            Class<? extends Annotation> qualifier) {
        for (InstanceHandle<T> handle : handles) {
            InjectableBean<T> bean = handle.getBean();
            boolean match = false;
            for (Annotation q : bean.getQualifiers()) {
                if (q.annotationType().equals(qualifier)) {
                    match = true;
                }
            }
            if (!match) {
                return handle.get();
            }
        }
        return null;
    }
}
