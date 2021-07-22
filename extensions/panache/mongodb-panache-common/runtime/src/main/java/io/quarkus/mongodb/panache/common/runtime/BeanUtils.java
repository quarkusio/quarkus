package io.quarkus.mongodb.panache.common.runtime;

import java.lang.annotation.Annotation;

import javax.inject.Named;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.runtime.MongoClientBeanUtil;
import io.quarkus.mongodb.runtime.MongoClientConfig;
import io.quarkus.mongodb.runtime.MongoClients;

public final class BeanUtils {

    private BeanUtils() {
    }

    public static String beanName(MongoEntity legacyEntity, io.quarkus.mongodb.panache.common.MongoEntity entity) {
        if (entity != null && !entity.clientName().isEmpty()) {
            return entity.clientName();
        }
        if (legacyEntity != null && !legacyEntity.clientName().isEmpty()) {
            return legacyEntity.clientName();
        }

        return MongoClientBeanUtil.DEFAULT_MONGOCLIENT_NAME;
    }

    public static <T> T clientFromArc(MongoEntity legacyEntity, io.quarkus.mongodb.panache.common.MongoEntity entity,
            Class<T> clientClass, boolean isReactive) {
        T mongoClient = Arc.container()
                .instance(clientClass, MongoClientBeanUtil.clientLiteral(beanName(legacyEntity, entity), isReactive))
                .get();
        if (mongoClient != null) {
            return mongoClient;
        }

        if ((entity == null || entity.clientName().isEmpty())
                && (legacyEntity == null || legacyEntity.clientName().isEmpty())) {
            // this case happens when there are multiple instances because they are all annotated with @Named
            for (InstanceHandle<T> handle : Arc.container().select(clientClass).handles()) {
                InjectableBean<T> bean = handle.getBean();
                boolean hasNamed = false;
                for (Annotation qualifier : bean.getQualifiers()) {
                    if (qualifier.annotationType().equals(Named.class)) {
                        hasNamed = true;
                    }
                }
                if (!hasNamed) {
                    return handle.get();
                }
            }
            throw new IllegalStateException(String.format("Unable to find default %s bean", clientClass.getSimpleName()));
        } else if (entity != null && !entity.clientName().isEmpty()) {
            throw new IllegalStateException(
                    String.format("Unable to find %s bean for entity %s", clientClass.getSimpleName(), entity));
        } else {
            throw new IllegalStateException(
                    String.format("Unable to find %s bean for entity %s", clientClass.getSimpleName(), legacyEntity));
        }
    }

    public static String getDatabaseName(MongoEntity legacyEntity, io.quarkus.mongodb.panache.common.MongoEntity mongoEntity,
            String clientBeanName) {
        MongoClients mongoClients = Arc.container().instance(MongoClients.class).get();
        MongoClientConfig matchingMongoClientConfig = mongoClients.getMatchingMongoClientConfig(clientBeanName);
        if (matchingMongoClientConfig.database.isPresent()) {
            return matchingMongoClientConfig.database.get();
        }

        if (!clientBeanName.equals(MongoClientBeanUtil.DEFAULT_MONGOCLIENT_NAME)) {
            MongoClientConfig defaultMongoClientConfig = mongoClients
                    .getMatchingMongoClientConfig(MongoClientBeanUtil.DEFAULT_MONGOCLIENT_NAME);
            if (defaultMongoClientConfig.database.isPresent()) {
                return defaultMongoClientConfig.database.get();
            }
        }

        if (legacyEntity == null && mongoEntity == null) {
            throw new IllegalArgumentException(
                    "The database property was not configured for the default Mongo Client (via 'quarkus.mongodb.database'");
        }
        if (legacyEntity != null && legacyEntity.clientName().isEmpty()) {
            throw new IllegalArgumentException("The database attribute was not set for the @MongoEntity annotation "
                    + "and neither was the database property configured for the default Mongo Client (via 'quarkus.mongodb.database')");
        }
        if (mongoEntity != null && mongoEntity.clientName().isEmpty()) {
            throw new IllegalArgumentException("The database attribute was not set for the @MongoEntity annotation "
                    + "and neither was the database property configured for the default Mongo Client (via 'quarkus.mongodb.database')");
        }
        if (legacyEntity != null) {
            throw new IllegalArgumentException(String.format(
                    "The database attribute was not set for the @MongoEntity annotation neither was the database property configured for the named Mongo Client (via 'quarkus.mongodb.%s.database')",
                    legacyEntity.clientName()));
        }
        throw new IllegalArgumentException(String.format(
                "The database attribute was not set for the @MongoEntity annotation neither was the database property configured for the named Mongo Client (via 'quarkus.mongodb.%s.database')",
                mongoEntity.clientName()));
    }
}
